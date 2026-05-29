package com.aigm.ai.service;

import com.aigm.common.condition.ConditionEvaluator;
import com.aigm.common.feign.dto.GameStateDTO;
import com.aigm.common.feign.dto.GenerateRequest;
import com.aigm.common.feign.dto.GenerateResponse;
import com.aigm.common.feign.dto.MemoryItem;
import com.aigm.common.feign.dto.NodeDTO;
import com.aigm.common.feign.dto.NpcDTO;
import com.aigm.common.feign.dto.NpcDialogue;
import com.aigm.common.feign.dto.ProposedTransition;
import com.aigm.common.feign.dto.StateChanges;
import com.aigm.common.feign.dto.TransitionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 无 api-key 时的确定性 STUB（基线 §5.13 验收/无 key 兜底）：不调用网络，
 * 按 currentNode + playerInput 关键词构造 schema 合法的 GenerateResponse，
 * 演示状态机推进。产出仍交 OutputValidator 二次校验/纠偏。
 */
@Slf4j
@Component
public class StubGenerator {

    public GenerateResponse generate(GenerateRequest req) {
        GenerateResponse out = new GenerateResponse();
        NodeDTO node = req.getCurrentNode();

        // 1) narrative：第二人称，引用节点标题/简述 + 玩家输入
        out.setNarrative(buildNarrative(req));

        // 2) npcDialogues：给第一个出场 NPC 一句贴合 persona 的话
        out.setNpcDialogues(buildDialogues(node, req.getNpcs()));

        // 3) stateChanges：按 playerInput 关键词触发
        StateChanges sc = buildStateChanges(req.getPlayerInput());
        out.setStateChanges(sc);

        // 4) proposedTransition：默认停留，仅当输入含移动意图才推进（符合「多数回合停留」领域模型）
        out.setProposedTransition(pickTransition(req.getGameState(), node, sc,
                req.getPlayerInput(), Boolean.TRUE.equals(req.getIsFirstTurn())));

        // 5) memoryToStore：一条 EVENT，importance 3
        out.setMemoryToStore(buildMemory(req));

        Map<String, Object> validation = new LinkedHashMap<>();
        validation.put("stub", true);
        validation.put("retryCount", 0);
        out.setValidation(validation);
        return out;
    }

    private String buildNarrative(GenerateRequest req) {
        NodeDTO node = req.getCurrentNode();
        String title = node != null && node.getTitle() != null ? node.getTitle() : "此地";
        String brief = node != null && node.getNarrativeBrief() != null ? node.getNarrativeBrief() : "";
        String briefHint = brief.length() > 60 ? brief.substring(0, 60) : brief;
        if (Boolean.TRUE.equals(req.getIsFirstTurn())) {
            return "你站在「" + title + "」之中，四周的气息扑面而来。" + briefHint
                    + " 你深吸一口气，决定迈出第一步——故事就此展开。";
        }
        String input = req.getPlayerInput() == null ? "" : req.getPlayerInput();
        return "你身处「" + title + "」。你尝试" + (input.isBlank() ? "环顾四周" : "「" + input + "」")
                + "，场景随之回应。" + briefHint + " 空气中似乎还藏着未被揭开的线索。";
    }

    private List<NpcDialogue> buildDialogues(NodeDTO node, List<NpcDTO> npcs) {
        List<NpcDialogue> list = new ArrayList<>();
        if (node == null || CollectionUtils.isEmpty(node.getNpcIds()) || CollectionUtils.isEmpty(npcs)) {
            return list;
        }
        Long firstNpcId = node.getNpcIds().get(0);
        NpcDTO npc = npcs.stream().filter(n -> firstNpcId.equals(n.getNpcId())).findFirst().orElse(null);
        if (npc == null) return list;
        NpcDialogue d = new NpcDialogue();
        d.setNpcId(npc.getNpcId());
        String name = npc.getName() == null ? "某人" : npc.getName();
        d.setLine(name + "缓缓开口：「……你来了。这里的事，恐怕没有表面那么简单。」");
        list.add(d);
        return list;
    }

    private StateChanges buildStateChanges(String playerInput) {
        StateChanges sc = emptyStateChanges();
        String input = playerInput == null ? "" : playerInput;
        if (input.contains("钥匙")) {
            sc.getSetFlags().add("has_key");
        }
        if (input.contains("日记")) {
            sc.getSetFlags().add("found_diary");
            sc.getAttrDelta().merge("evidence", 1, Integer::sum);
        }
        if (input.contains("手术刀") || input.contains("凶器")) {
            sc.getSetFlags().add("found_weapon");
            sc.getAttrDelta().merge("evidence", 1, Integer::sum);
        }
        return sc;
    }

    /** 移动意图关键词：仅当玩家输入显式表达推进时才提议跳转，否则停留（领域模型：多数回合是停留）。 */
    private static final String[] MOVE_VERBS = {
            "去", "前往", "进入", "上楼", "下楼", "对峙", "指认", "揭露", "离开", "返回",
            "回到", "走向", "走进", "推门", "继续", "前进", "来到", "上去", "下去", "出发"
    };

    /**
     * 选择跳转：默认停留(null)。仅当首回合(开局入场)或输入含移动意图时，在「应用变更后 condition 为真」的
     * 转移中，优先选描述与输入字符二元组重合最多者，其次取优先级最高者。
     */
    private ProposedTransition pickTransition(GameStateDTO gs, NodeDTO node, StateChanges sc,
                                              String playerInput, boolean firstTurn) {
        ProposedTransition pt = new ProposedTransition();
        if (node == null || CollectionUtils.isEmpty(node.getTransitions())) {
            pt.setToNodeId(null);
            pt.setReason("当前节点无出边，停留。");
            return pt;
        }
        Map<String, Boolean> flags = simulateFlags(gs, sc);
        Map<String, Integer> attrs = simulateAttrs(gs, sc);
        List<String> inventory = simulateInventory(gs, sc);

        List<TransitionDTO> satisfiable = node.getTransitions().stream()
                .filter(t -> t.getToNodeId() != null)
                .filter(t -> ConditionEvaluator.evaluate(t.getCondition(), flags, attrs, inventory))
                .toList();
        if (satisfiable.isEmpty()) {
            pt.setToNodeId(null);
            pt.setReason("当前条件尚不足以触发任何合法转移，停留当前节点。");
            return pt;
        }

        String input = playerInput == null ? "" : playerInput;
        boolean wantMove = firstTurn || containsMoveVerb(input);
        if (!wantMove) {
            pt.setToNodeId(null);
            pt.setReason("玩家未表达推进意图，停留当前节点继续即兴。");
            return pt;
        }

        TransitionDTO best = satisfiable.stream()
                .max(Comparator
                        .comparingInt((TransitionDTO t) -> descMatchScore(t.getDescription(), input))
                        .thenComparingInt(t -> t.getPriority() == null ? 0 : t.getPriority()))
                .orElse(satisfiable.get(0));

        pt.setToNodeId(best.getToNodeId());
        pt.setReason("玩家表达推进意图，满足条件[" + (best.getCondition() == null ? "always" : best.getCondition())
                + "]，推进到节点 " + best.getToNodeId() + "。");
        return pt;
    }

    private boolean containsMoveVerb(String input) {
        for (String v : MOVE_VERBS) {
            if (input.contains(v)) return true;
        }
        return false;
    }

    /** 描述与输入的字符二元组重合数，用于把玩家意图匹配到最贴切的出边。 */
    private int descMatchScore(String description, String input) {
        if (description == null || description.isBlank() || input.isBlank()) return 0;
        int score = 0;
        for (int i = 0; i + 1 < description.length(); i++) {
            if (input.contains(description.substring(i, i + 2))) score++;
        }
        return score;
    }

    private List<MemoryItem> buildMemory(GenerateRequest req) {
        List<MemoryItem> list = new ArrayList<>();
        MemoryItem mi = new MemoryItem();
        String title = req.getCurrentNode() != null ? req.getCurrentNode().getTitle() : "未知场景";
        String input = req.getPlayerInput() == null ? "（开局）" : req.getPlayerInput();
        mi.setContent("在「" + title + "」，玩家行动：" + input);
        mi.setMemType("EVENT");
        mi.setImportance(3);
        list.add(mi);
        return list;
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

    private Map<String, Boolean> simulateFlags(GameStateDTO gs, StateChanges sc) {
        Map<String, Boolean> flags = new HashMap<>();
        if (gs != null && gs.getFlags() != null) flags.putAll(gs.getFlags());
        if (sc.getSetFlags() != null) sc.getSetFlags().forEach(f -> flags.put(f, true));
        if (sc.getClearFlags() != null) sc.getClearFlags().forEach(f -> flags.put(f, false));
        return flags;
    }

    private Map<String, Integer> simulateAttrs(GameStateDTO gs, StateChanges sc) {
        Map<String, Integer> attrs = new HashMap<>();
        if (gs != null && gs.getAttributes() != null) attrs.putAll(gs.getAttributes());
        if (sc.getAttrDelta() != null) sc.getAttrDelta().forEach((k, v) -> attrs.merge(k, v, Integer::sum));
        return attrs;
    }

    private List<String> simulateInventory(GameStateDTO gs, StateChanges sc) {
        List<String> inv = new ArrayList<>(gs == null || gs.getInventory() == null ? List.of() : gs.getInventory());
        if (sc.getAddItems() != null) sc.getAddItems().forEach(it -> { if (!inv.contains(it)) inv.add(it); });
        if (sc.getRemoveItems() != null) inv.removeAll(sc.getRemoveItems());
        return inv;
    }
}
