package com.aigm.ai.service;

import com.aigm.common.condition.ConditionEvaluator;
import com.aigm.common.feign.dto.GameStateDTO;
import com.aigm.common.feign.dto.GenerateRequest;
import com.aigm.common.feign.dto.GenerateResponse;
import com.aigm.common.feign.dto.MemoryItem;
import com.aigm.common.feign.dto.NpcDialogue;
import com.aigm.common.feign.dto.ProposedTransition;
import com.aigm.common.feign.dto.StateChanges;
import com.aigm.common.feign.dto.TransitionDTO;
import com.aigm.common.result.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 输出解析 + schema 校验 + 白名单预校验 + clamp + 兜底（基线 §6.5，ai-engine 预校验侧）。
 * 最终权威校验在 game-service。validation 为非契约 Map 调试信息。
 *
 * <p>两条入口：
 * <ul>
 *   <li>{@link #validate(LlmClient.Result, GenerateRequest)}：校验 LLM 原始 JSON 文本（真实路径）；</li>
 *   <li>{@link #validateStructured(GenerateResponse, GenerateRequest)}：校验已构造好的对象（STUB 路径）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutputValidator {

    private final ObjectMapper objectMapper;

    private static final int ATTR_MIN = 0;
    private static final int ATTR_MAX = 100;

    // ============ 真实 LLM JSON 路径 ============
    public GenerateResponse validate(LlmClient.Result llmResult, GenerateRequest req) {
        GenerateResponse out = new GenerateResponse();
        Map<String, Object> vi = newValidation();
        out.setValidation(vi);
        vi.put("retryCount", llmResult.retryCount());

        // 步骤1：JSON 合法性（§6.5-1）
        if (!llmResult.parseable()) {
            vi.put("jsonValid", false);
            vi.put("schemaValid", false);
            vi.put("rejectCode", ResultCode.AI_LLM_BAD_JSON.getCode()); // 1501
            return fallback(out, req, "LLM 输出非合法 JSON，已降级兜底");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(llmResult.json());
        } catch (Exception e) {
            vi.put("jsonValid", false);
            vi.put("rejectCode", ResultCode.AI_LLM_BAD_JSON.getCode());
            return fallback(out, req, "JSON 解析异常，已降级兜底");
        }

        // 步骤2：Schema 校验（§6.5-2）
        String narrative = text(root, "narrative");
        if (narrative.isBlank()) {
            vi.put("schemaValid", false);
            addWarn(vi, "narrative 缺失/为空，已用兜底文案");
            narrative = defaultNarrative(req);
        }
        out.setNarrative(narrative);
        out.setNpcDialogues(parseDialogues(root.get("npcDialogues"), req, vi));
        out.setStateChanges(parseStateChanges(root.get("stateChanges"), vi));
        out.setMemoryToStore(parseMemories(root.get("memoryToStore")));

        // 步骤3：白名单预校验（§6.5-3）
        ProposedTransition pt = parseProposed(root.get("proposedTransition"));
        applyWhitelist(out, req, pt, vi);

        // 步骤4：属性 clamp（§6.5-4，非必须的 sanity clamp）
        clampAttrDelta(out, req, vi);

        return out;
    }

    // ============ STUB 已构造对象路径 ============
    public GenerateResponse validateStructured(GenerateResponse out, GenerateRequest req) {
        Map<String, Object> vi = out.getValidation() != null ? out.getValidation() : newValidation();
        out.setValidation(vi);

        if (out.getNarrative() == null || out.getNarrative().isBlank()) {
            addWarn(vi, "narrative 缺失，已用兜底文案");
            out.setNarrative(defaultNarrative(req));
        }
        // 复用 npcId 校验：丢弃不在出场列表的对白
        out.setNpcDialogues(filterDialogues(out.getNpcDialogues(), req, vi));
        out.setStateChanges(normalizeStateChanges(out.getStateChanges()));
        if (out.getMemoryToStore() == null) out.setMemoryToStore(new ArrayList<>());

        applyWhitelist(out, req, out.getProposedTransition(), vi);
        clampAttrDelta(out, req, vi);
        return out;
    }

    // ---------- 白名单预校验 ----------
    private void applyWhitelist(GenerateResponse out, GenerateRequest req,
                                ProposedTransition pt, Map<String, Object> vi) {
        if (pt == null || pt.getToNodeId() == null) {
            out.setProposedTransition(null); // 停留
            return;
        }
        Long target = pt.getToNodeId();
        TransitionDTO matched = null;
        if (req.getCurrentNode() != null && req.getCurrentNode().getTransitions() != null) {
            for (TransitionDTO t : req.getCurrentNode().getTransitions()) {
                if (Objects.equals(t.getToNodeId(), target)) { matched = t; break; }
            }
        }
        if (matched == null) {
            vi.put("transitionAccepted", false);
            vi.put("rejectCode", ResultCode.AI_TRANSITION_REJECTED.getCode()); // 1503
            addWarn(vi, "proposedTransition.toNodeId=" + target + " 不在白名单，已拒绝并停留");
            out.setProposedTransition(null);
            return;
        }
        Map<String, Boolean> flags = simulateFlags(req.getGameState(), out.getStateChanges());
        Map<String, Integer> attrs = simulateAttrs(req.getGameState(), out.getStateChanges());
        List<String> inventory = simulateInventory(req.getGameState(), out.getStateChanges());
        if (!ConditionEvaluator.evaluate(matched.getCondition(), flags, attrs, inventory)) {
            vi.put("transitionAccepted", false);
            vi.put("rejectCode", ResultCode.AI_TRANSITION_REJECTED.getCode());
            addWarn(vi, "转移 " + target + " 条件[" + matched.getCondition() + "]未满足，已拒绝并停留");
            out.setProposedTransition(null);
            return;
        }
        out.setProposedTransition(pt);
    }

    private Map<String, Boolean> simulateFlags(GameStateDTO gs, StateChanges sc) {
        Map<String, Boolean> flags = new HashMap<>();
        if (gs != null && gs.getFlags() != null) flags.putAll(gs.getFlags());
        if (sc != null && sc.getSetFlags() != null) sc.getSetFlags().forEach(f -> flags.put(f, true));
        if (sc != null && sc.getClearFlags() != null) sc.getClearFlags().forEach(f -> flags.put(f, false));
        return flags;
    }

    private Map<String, Integer> simulateAttrs(GameStateDTO gs, StateChanges sc) {
        Map<String, Integer> attrs = new HashMap<>();
        if (gs != null && gs.getAttributes() != null) attrs.putAll(gs.getAttributes());
        if (sc != null && sc.getAttrDelta() != null) sc.getAttrDelta().forEach((k, v) -> attrs.merge(k, v, Integer::sum));
        return attrs;
    }

    private List<String> simulateInventory(GameStateDTO gs, StateChanges sc) {
        List<String> inv = new ArrayList<>(gs == null || gs.getInventory() == null ? List.of() : gs.getInventory());
        if (sc != null && sc.getAddItems() != null) sc.getAddItems().forEach(it -> { if (!inv.contains(it)) inv.add(it); });
        if (sc != null && sc.getRemoveItems() != null) inv.removeAll(sc.getRemoveItems());
        return inv;
    }

    private void clampAttrDelta(GenerateResponse out, GenerateRequest req, Map<String, Object> vi) {
        Map<String, Integer> base = req.getGameState() == null || req.getGameState().getAttributes() == null
                ? Map.of() : req.getGameState().getAttributes();
        StateChanges sc = out.getStateChanges();
        if (sc == null || sc.getAttrDelta() == null) return;
        Map<String, Integer> delta = sc.getAttrDelta();
        for (Map.Entry<String, Integer> en : new HashMap<>(delta).entrySet()) {
            int cur = base.getOrDefault(en.getKey(), 0);
            int after = cur + en.getValue();
            int clamped = Math.max(ATTR_MIN, Math.min(ATTR_MAX, after));
            if (clamped != after) {
                addWarn(vi, "属性 " + en.getKey() + " 越界，已 clamp 到 " + clamped);
                delta.put(en.getKey(), clamped - cur);
            }
        }
    }

    // ---------- 解析辅助 ----------
    private List<NpcDialogue> parseDialogues(JsonNode dlgs, GenerateRequest req, Map<String, Object> vi) {
        List<NpcDialogue> list = new ArrayList<>();
        Set<Long> allowed = allowedNpcIds(req);
        if (dlgs != null && dlgs.isArray()) {
            for (JsonNode d : dlgs) {
                if (d.hasNonNull("npcId") && d.hasNonNull("line")) {
                    long npcId = d.get("npcId").asLong();
                    if (allowed.contains(npcId)) {
                        NpcDialogue nd = new NpcDialogue();
                        nd.setNpcId(npcId);
                        nd.setLine(d.get("line").asText());
                        list.add(nd);
                    } else {
                        markSchemaInvalid(vi, "丢弃非法 npcId=" + npcId + "（不在出场列表）");
                    }
                }
            }
        }
        return list;
    }

    private List<NpcDialogue> filterDialogues(List<NpcDialogue> in, GenerateRequest req, Map<String, Object> vi) {
        List<NpcDialogue> list = new ArrayList<>();
        if (in == null) return list;
        Set<Long> allowed = allowedNpcIds(req);
        for (NpcDialogue d : in) {
            if (d != null && d.getNpcId() != null && allowed.contains(d.getNpcId()) && d.getLine() != null) {
                list.add(d);
            } else if (d != null) {
                markSchemaInvalid(vi, "丢弃非法 npcId=" + (d.getNpcId()) + "（不在出场列表）");
            }
        }
        return list;
    }

    private Set<Long> allowedNpcIds(GenerateRequest req) {
        if (req.getCurrentNode() == null || req.getCurrentNode().getNpcIds() == null) return new HashSet<>();
        return new HashSet<>(req.getCurrentNode().getNpcIds());
    }

    private StateChanges parseStateChanges(JsonNode n, Map<String, Object> vi) {
        StateChanges sc = emptyStateChanges();
        if (n == null || !n.isObject()) {
            markSchemaInvalid(vi, "stateChanges 缺失，已补空");
            return sc;
        }
        sc.setSetFlags(strList(n.get("setFlags")));
        sc.setClearFlags(strList(n.get("clearFlags")));
        sc.setAddItems(strList(n.get("addItems")));
        sc.setRemoveItems(strList(n.get("removeItems")));
        Map<String, Integer> attr = new LinkedHashMap<>();
        JsonNode ad = n.get("attrDelta");
        if (ad != null && ad.isObject()) {
            ad.fields().forEachRemaining(e -> {
                if (e.getValue().canConvertToInt()) attr.put(e.getKey(), e.getValue().asInt());
            });
        }
        sc.setAttrDelta(attr);
        return sc;
    }

    private StateChanges normalizeStateChanges(StateChanges sc) {
        if (sc == null) return emptyStateChanges();
        if (sc.getSetFlags() == null) sc.setSetFlags(new ArrayList<>());
        if (sc.getClearFlags() == null) sc.setClearFlags(new ArrayList<>());
        if (sc.getAddItems() == null) sc.setAddItems(new ArrayList<>());
        if (sc.getRemoveItems() == null) sc.setRemoveItems(new ArrayList<>());
        if (sc.getAttrDelta() == null) sc.setAttrDelta(new LinkedHashMap<>());
        return sc;
    }

    private List<MemoryItem> parseMemories(JsonNode n) {
        List<MemoryItem> list = new ArrayList<>();
        if (n == null || !n.isArray()) return list;
        for (JsonNode m : n) {
            if (m.hasNonNull("content")) {
                MemoryItem it = new MemoryItem();
                it.setContent(m.get("content").asText());
                it.setMemType(m.hasNonNull("memType") ? m.get("memType").asText() : "EVENT");
                int imp = m.hasNonNull("importance") ? m.get("importance").asInt(3) : 3;
                it.setImportance(Math.max(1, Math.min(5, imp)));
                list.add(it);
            }
        }
        return list;
    }

    private ProposedTransition parseProposed(JsonNode n) {
        ProposedTransition pt = new ProposedTransition();
        if (n == null || !n.isObject()) { pt.setToNodeId(null); return pt; }
        JsonNode tn = n.get("toNodeId");
        if (tn != null && !tn.isNull() && tn.canConvertToLong()) {
            pt.setToNodeId(tn.asLong());
        } else {
            pt.setToNodeId(null);
        }
        pt.setReason(n.hasNonNull("reason") ? n.get("reason").asText() : null);
        return pt;
    }

    private List<String> strList(JsonNode n) {
        List<String> list = new ArrayList<>();
        if (n != null && n.isArray()) n.forEach(x -> { if (!x.isNull()) list.add(x.asText()); });
        return list;
    }

    private String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return (n == null || n.isNull()) ? "" : n.asText("");
    }

    // ---------- 兜底降级（§6.5-1 末句） ----------
    private GenerateResponse fallback(GenerateResponse out, GenerateRequest req, String msg) {
        Map<String, Object> vi = out.getValidation();
        vi.put("fallback", true);
        addWarn(vi, msg);
        if (out.getNarrative() == null || out.getNarrative().isBlank()) {
            out.setNarrative(defaultNarrative(req));
        }
        out.setProposedTransition(null);
        out.setStateChanges(emptyStateChanges());
        out.setMemoryToStore(new ArrayList<>());
        if (out.getNpcDialogues() == null) out.setNpcDialogues(new ArrayList<>());
        log.warn("[VALIDATE] session={} FALLBACK: {}", req.getSessionId(), msg);
        return out;
    }

    private String defaultNarrative(GenerateRequest req) {
        String title = req.getCurrentNode() != null && req.getCurrentNode().getTitle() != null
                ? req.getCurrentNode().getTitle() : "此地";
        return "你环顾四周（" + title + "），一时间没有新的变化。空气安静下来，你可以再尝试一个更明确的行动。";
    }

    // ---------- validation Map 辅助 ----------
    private Map<String, Object> newValidation() {
        Map<String, Object> vi = new LinkedHashMap<>();
        vi.put("jsonValid", true);
        vi.put("schemaValid", true);
        vi.put("transitionAccepted", true);
        vi.put("fallback", false);
        vi.put("retryCount", 0);
        vi.put("warnings", new ArrayList<String>());
        return vi;
    }

    @SuppressWarnings("unchecked")
    private void addWarn(Map<String, Object> vi, String w) {
        Object ws = vi.computeIfAbsent("warnings", k -> new ArrayList<String>());
        if (ws instanceof List) ((List<String>) ws).add(w);
        log.warn("[VALIDATE] {}", w);
    }

    private void markSchemaInvalid(Map<String, Object> vi, String w) {
        vi.put("schemaValid", false);
        vi.putIfAbsent("rejectCode", ResultCode.AI_OUTPUT_SCHEMA_INVALID.getCode()); // 1502
        addWarn(vi, w);
    }

    private StateChanges emptyStateChanges() {
        StateChanges sc = new StateChanges();
        sc.setSetFlags(new ArrayList<>());
        sc.setClearFlags(new ArrayList<>());
        sc.setAddItems(new ArrayList<>());
        sc.setRemoveItems(new ArrayList<>());
        sc.setAttrDelta(new LinkedHashMap<>());
        return sc;
    }
}
