package com.aigm.game.service;

import com.aigm.common.exception.BizException;
import com.aigm.common.feign.AiEngineClient;
import com.aigm.common.feign.MemoryClient;
import com.aigm.common.feign.ScenarioClient;
import com.aigm.common.feign.dto.*;
import com.aigm.common.result.R;
import com.aigm.common.result.ResultCode;
import com.aigm.common.util.PageQuery;
import com.aigm.common.util.PageResult;
import com.aigm.game.dto.*;
import com.aigm.game.entity.GameSession;
import com.aigm.game.entity.GameState;
import com.aigm.game.entity.Turn;
import com.aigm.game.mapper.GameSessionMapper;
import com.aigm.game.mapper.GameStateMapper;
import com.aigm.game.mapper.TurnMapper;
import com.aigm.game.util.StateCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** 编排核心：开局 + 回合六步编排 + 白名单二次校验 + 事务落库（基线 §6/§8/§9/§10/§11）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_WIN = 2;
    private static final int STATUS_LOSE = 3;
    private static final int STATUS_ABANDONED = 4;
    private static final int SECRET_UNLOCK_EVIDENCE = 1; // ADR-0004：evidence>=1 解锁 NPC secret
    private static final int SUMMARY_MAX = 1000;
    private static final com.fasterxml.jackson.databind.ObjectMapper OM =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final GameSessionMapper sessionMapper;
    private final GameStateMapper stateMapper;
    private final TurnMapper turnMapper;
    private final ScenarioClient scenarioClient;
    private final AiEngineClient aiEngineClient;
    private final MemoryClient memoryClient;
    private final TransitionWhitelist whitelist;
    private final TurnPersister turnPersister;

    // ============ 开局 ============
    public StartSessionVO startSession(Long userId, Long scenarioId) {
        ScenarioRunVO sc = unwrap(scenarioClient.getRunScenario(scenarioId), ResultCode.RESOURCE_NOT_FOUND);
        if (sc.getStartNodeId() == null) {
            throw new BizException(ResultCode.SCENARIO_NO_START_NODE);
        }
        GameSession session = new GameSession();
        session.setUserId(userId);
        session.setScenarioId(scenarioId);
        session.setTitle(sc.getTitle() + " " + LocalDate.now());
        session.setStatus(STATUS_RUNNING);
        session.setTurnCount(0);
        sessionMapper.insert(session);

        GameState state = new GameState();
        state.setSessionId(session.getId());
        state.setCurrentNodeId(sc.getStartNodeId());
        state.setFlags("{}");
        state.setInventory("[]");
        state.setAttributes(StateCodec.toJson(Map.of("sanity", 80, "evidence", 0)));
        state.setRecentSummary("");
        stateMapper.insert(state);

        SceneNodeRunVO node = unwrap(
                scenarioClient.getRunNode(scenarioId, sc.getStartNodeId()), ResultCode.RESOURCE_NOT_FOUND);

        TurnResultVO result = runTurn(session, state, sc, node, null, true);

        StartSessionVO vo = new StartSessionVO();
        vo.setSessionId(session.getId());
        vo.setState(result.getState());
        vo.setFirstTurn(result.getTurn().getAiOutput());
        return vo;
    }

    // ============ 回合（六步编排）============
    public TurnResultVO submitTurn(Long userId, Long sessionId, String playerInput) {
        // ① 取状态 + 归属/状态校验
        GameSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BizException(ResultCode.GAME_SESSION_NOT_FOUND);
        if (!Objects.equals(session.getUserId(), userId)) throw new BizException(ResultCode.GAME_SESSION_FORBIDDEN);
        if (session.getStatus() != STATUS_RUNNING) throw new BizException(ResultCode.STATE_INVALID);
        GameState state = stateMapper.selectBySessionId(sessionId);

        // ② 取当前节点 + NPC + 白名单（运行时只读内部接口）
        SceneNodeRunVO node = unwrap(
                scenarioClient.getRunNode(session.getScenarioId(), state.getCurrentNodeId()),
                ResultCode.RESOURCE_NOT_FOUND);
        ScenarioRunVO sc = unwrap(scenarioClient.getRunScenario(session.getScenarioId()), ResultCode.RESOURCE_NOT_FOUND);

        return runTurn(session, state, sc, node, playerInput, false);
    }

    /** 步骤 ③④⑤⑥：召回→调AI→白名单权威校验→落库（外部慢调用在事务外）。 */
    private TurnResultVO runTurn(GameSession session, GameState state, ScenarioRunVO sc,
                                 SceneNodeRunVO node, String playerInput, boolean firstTurn) {
        // ③ RAG 召回（弱依赖，失败静默降级）
        List<RecalledMemory> memories = recallSafely(session.getId(),
                playerInput == null ? node.getNarrativeBrief() : playerInput);

        // ④ 调 ai-engine（失败阻断回合）
        GenerateRequest req = buildRequest(session, state, sc, node, memories, playerInput, firstTurn);
        GenerateResponse ai = generate(req);

        // ⑤ 应用 stateChanges（含 clamp）→ 白名单权威二次校验
        Map<String, Boolean> flags = StateCodec.readFlags(state.getFlags());
        Map<String, Integer> attrs = StateCodec.readAttributes(state.getAttributes());
        List<String> inventory = StateCodec.readInventory(state.getInventory());
        applyStateChanges(flags, attrs, inventory, ai.getStateChanges());

        Long acceptedTo = whitelist.resolve(node, ai.getProposedTransition(), flags, attrs, inventory);
        boolean finished = false;
        int newStatus = STATUS_RUNNING;
        Long newNodeId = state.getCurrentNodeId();
        if (acceptedTo != null) {
            newNodeId = acceptedTo;
            // 取目标节点元信息判定结局（事务外 Feign，保持事务短）；取不到则降级为非结局，不阻断落库
            try {
                SceneNodeRunVO target = unwrap(
                        scenarioClient.getRunNode(session.getScenarioId(), acceptedTo), ResultCode.RESOURCE_NOT_FOUND);
                if (Boolean.TRUE.equals(target.getIsEnding())) {
                    newStatus = "WIN".equalsIgnoreCase(target.getEndingType()) ? STATUS_WIN : STATUS_LOSE;
                    finished = true;
                }
            } catch (Exception e) {
                log.warn("[degrade] 取目标节点 {} 结局信息失败，按非结局推进。session={}", acceptedTo, session.getId());
            }
        }

        // 组装最终 state
        state.setFlags(StateCodec.toJson(flags));
        state.setAttributes(StateCodec.toJson(attrs));
        state.setInventory(StateCodec.toJson(inventory));
        state.setCurrentNodeId(newNodeId);
        state.setRecentSummary(rollingSummary(state.getRecentSummary(), ai.getNarrative()));

        // ⑥ 落库（事务内只做本地 DB 写，独立 Bean 保证事务生效）
        Turn turn = turnPersister.persist(session, state, playerInput, ai, newStatus);

        // 事务提交后存记忆（尽力而为）
        storeSafely(session.getId(), ai.getMemoryToStore());

        TurnResultVO result = new TurnResultVO();
        result.setTurn(toTurnVO(turn.getTurnNo(), playerInput, ai));
        result.setState(toStateVO(state));
        result.setFinished(finished);
        return result;
    }

    // ============ 读档 / 列表 / 弃局 ============
    public SessionDetailVO getSessionDetail(Long userId, Long sessionId) {
        GameSession session = requireOwnedSession(userId, sessionId);
        GameState state = stateMapper.selectBySessionId(sessionId);
        List<Turn> turns = turnMapper.selectBySessionOrderByTurnNo(sessionId);
        SessionDetailVO vo = new SessionDetailVO();
        vo.setSession(toSessionVO(session));
        vo.setState(toStateVO(state));
        List<TurnVO> turnVOs = new ArrayList<>();
        for (Turn t : turns) {
            turnVOs.add(toTurnVO(t.getTurnNo(), t.getPlayerInput(), parseAi(t.getAiOutput())));
        }
        vo.setTurns(turnVOs);
        return vo;
    }

    public PageResult<SessionVO> pageSessions(Long userId, PageQuery pq, Integer status) {
        Page<GameSession> page = new Page<>(pq.getPage(), pq.getSize());
        LambdaQueryWrapper<GameSession> qw = new LambdaQueryWrapper<GameSession>()
                .eq(GameSession::getUserId, userId)
                .orderByDesc(GameSession::getUpdatedAt);
        if (status != null) qw.eq(GameSession::getStatus, status);
        Page<GameSession> result = sessionMapper.selectPage(page, qw);
        List<SessionVO> list = result.getRecords().stream().map(this::toSessionVO).toList();
        return PageResult.of(list, result.getTotal(), pq.getPage(), pq.getSize());
    }

    public void abandon(Long userId, Long sessionId) {
        GameSession session = requireOwnedSession(userId, sessionId);
        if (session.getStatus() == STATUS_RUNNING) {
            session.setStatus(STATUS_ABANDONED);
            sessionMapper.updateById(session);
        }
    }

    public GameStateVO getState(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        return toStateVO(stateMapper.selectBySessionId(sessionId));
    }

    // ============ 私有工具 ============
    private GameSession requireOwnedSession(Long userId, Long sessionId) {
        GameSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BizException(ResultCode.GAME_SESSION_NOT_FOUND);
        if (!Objects.equals(session.getUserId(), userId)) throw new BizException(ResultCode.GAME_SESSION_FORBIDDEN);
        return session;
    }

    private GenerateRequest buildRequest(GameSession session, GameState state, ScenarioRunVO sc,
                                         SceneNodeRunVO node, List<RecalledMemory> memories,
                                         String playerInput, boolean firstTurn) {
        GenerateRequest req = new GenerateRequest();
        req.setSessionId(session.getId());
        ScenarioContext ctx = new ScenarioContext();
        ctx.setTitle(sc.getTitle());
        ctx.setGenre(sc.getGenre());
        req.setScenarioContext(ctx);

        Map<String, Integer> attrs = StateCodec.readAttributes(state.getAttributes());
        GameStateDTO gs = new GameStateDTO();
        gs.setCurrentNodeId(state.getCurrentNodeId());
        gs.setFlags(StateCodec.readFlags(state.getFlags()));
        gs.setInventory(StateCodec.readInventory(state.getInventory()));
        gs.setAttributes(attrs);
        gs.setRecentSummary(state.getRecentSummary());
        req.setGameState(gs);

        NodeDTO nd = new NodeDTO();
        nd.setId(node.getId());
        nd.setNodeKey(node.getNodeKey());
        nd.setTitle(node.getTitle());
        nd.setNarrativeBrief(node.getNarrativeBrief());
        nd.setIsEnding(node.getIsEnding());
        nd.setEndingType(node.getEndingType());
        nd.setTransitions(node.getTransitions());
        List<NpcRunVO> nodeNpcs = node.getNpcs() == null ? List.of() : node.getNpcs();
        nd.setNpcIds(nodeNpcs.stream().map(NpcRunVO::getNpcId).toList());
        req.setCurrentNode(nd);

        List<NpcDTO> npcs = new ArrayList<>();
        for (NpcRunVO n : nodeNpcs) {
            NpcDTO d = new NpcDTO();
            d.setNpcId(n.getNpcId());
            d.setNpcKey(n.getNpcKey());
            d.setName(n.getName());
            d.setPersona(n.getPersona());
            d.setKnownFacts(resolveKnownFacts(n, attrs));
            npcs.add(d);
        }
        req.setNpcs(npcs);
        req.setRecalledMemories(memories);
        req.setPlayerInput(playerInput);
        req.setIsFirstTurn(firstTurn);
        return req;
    }

    /** 按 evidence 阈值动态解锁 NPC secret 并入 knownFacts（基线 §6.3 / ADR-0004）。 */
    private String resolveKnownFacts(NpcRunVO npc, Map<String, Integer> attrs) {
        String base = npc.getBackground() == null ? "" : npc.getBackground();
        int evidence = attrs.getOrDefault("evidence", 0);
        if (evidence >= SECRET_UNLOCK_EVIDENCE && npc.getSecret() != null && !npc.getSecret().isBlank()) {
            return base.isBlank() ? npc.getSecret() : base + " " + npc.getSecret();
        }
        return base;
    }

    private void applyStateChanges(Map<String, Boolean> flags, Map<String, Integer> attrs,
                                   List<String> inventory, StateChanges sc) {
        if (sc == null) return;
        if (sc.getSetFlags() != null) sc.getSetFlags().forEach(f -> flags.put(f, true));
        if (sc.getClearFlags() != null) sc.getClearFlags().forEach(f -> flags.put(f, false));
        if (sc.getAddItems() != null) sc.getAddItems().forEach(i -> {
            if (!inventory.contains(i)) inventory.add(i);
        });
        if (sc.getRemoveItems() != null) inventory.removeAll(sc.getRemoveItems());
        if (sc.getAttrDelta() != null) {
            sc.getAttrDelta().forEach((k, delta) -> {
                int cur = attrs.getOrDefault(k, 0) + (delta == null ? 0 : delta);
                cur = Math.max(0, cur);                              // 通用下界，防 AI 写负
                if ("sanity".equals(k)) cur = Math.min(100, cur);   // sanity 上界 100
                attrs.put(k, cur);
            });
        }
    }

    private String rollingSummary(String old, String narrative) {
        String merged = ((old == null ? "" : old) + " " + (narrative == null ? "" : narrative)).trim();
        if (merged.length() > SUMMARY_MAX) {
            merged = merged.substring(merged.length() - SUMMARY_MAX);
        }
        return merged;
    }

    private GenerateResponse generate(GenerateRequest req) {
        try {
            R<GenerateResponse> r = aiEngineClient.generate(req);
            if (r == null || r.getData() == null) throw new BizException(ResultCode.AI_LLM_FAILED);
            return r.getData();
        } catch (BizException be) {
            throw be;
        } catch (Exception e) {
            log.warn("[degrade] ai-engine generate failed", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "GM 正在打盹，请稍后再试");
        }
    }

    private List<RecalledMemory> recallSafely(Long sessionId, String query) {
        try {
            RecallRequest rr = new RecallRequest();
            rr.setSessionId(sessionId);
            rr.setQuery(query);
            rr.setTopK(5);
            R<RecallResult> r = memoryClient.recall(rr);
            if (r != null && r.getData() != null && r.getData().getMemories() != null) {
                return r.getData().getMemories();
            }
        } catch (Exception e) {
            log.warn("[degrade] recall failed, continue without memories. session={}", sessionId);
        }
        return Collections.emptyList();
    }

    private void storeSafely(Long sessionId, List<MemoryItem> items) {
        if (items == null || items.isEmpty()) return;
        try {
            StoreRequest sr = new StoreRequest();
            sr.setSessionId(sessionId);
            sr.setItems(items);
            memoryClient.store(sr);
        } catch (Exception e) {
            log.warn("[degrade] memory store failed, skip. session={}", sessionId);
        }
    }

    private <T> T unwrap(R<T> r, ResultCode notFound) {
        if (r == null) throw new BizException(ResultCode.SERVICE_UNAVAILABLE);
        if (r.getCode() != 0) throw new BizException(r.getCode(), r.getMessage());
        if (r.getData() == null) throw new BizException(notFound);
        return r.getData();
    }

    private GameStateVO toStateVO(GameState s) {
        GameStateVO vo = new GameStateVO();
        vo.setCurrentNodeId(s.getCurrentNodeId());
        vo.setFlags(StateCodec.readFlags(s.getFlags()));
        vo.setInventory(StateCodec.readInventory(s.getInventory()));
        vo.setAttributes(StateCodec.readAttributes(s.getAttributes()));
        vo.setRecentSummary(s.getRecentSummary());
        return vo;
    }

    private SessionVO toSessionVO(GameSession s) {
        SessionVO vo = new SessionVO();
        vo.setSessionId(s.getId());
        vo.setScenarioId(s.getScenarioId());
        vo.setTitle(s.getTitle());
        vo.setStatus(s.getStatus());
        vo.setTurnCount(s.getTurnCount());
        vo.setCreatedAt(s.getCreatedAt());
        vo.setUpdatedAt(s.getUpdatedAt());
        return vo;
    }

    private TurnVO toTurnVO(Integer turnNo, String playerInput, GenerateResponse ai) {
        TurnVO vo = new TurnVO();
        vo.setTurnNo(turnNo);
        vo.setPlayerInput(playerInput);
        vo.setAiOutput(ai);
        return vo;
    }

    private GenerateResponse parseAi(String json) {
        try {
            return OM.readValue(json, GenerateResponse.class);
        } catch (Exception e) {
            return null;
        }
    }
}
