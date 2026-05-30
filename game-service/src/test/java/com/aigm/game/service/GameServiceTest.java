package com.aigm.game.service;

import com.aigm.common.exception.BizException;
import com.aigm.common.feign.AiEngineClient;
import com.aigm.common.feign.MemoryClient;
import com.aigm.common.feign.ScenarioClient;
import com.aigm.common.feign.dto.*;
import com.aigm.common.result.R;
import com.aigm.game.dto.TurnResultVO;
import com.aigm.game.entity.GameSession;
import com.aigm.game.entity.GameState;
import com.aigm.game.entity.Turn;
import com.aigm.game.mapper.GameSessionMapper;
import com.aigm.game.mapper.GameStateMapper;
import com.aigm.game.mapper.TurnMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 回合六步编排单元测试（基线 §8/§9/§10）。用 Mockito 桩掉三个 Feign 下游 + Mapper + TurnPersister，
 * 但用<b>真实</b> {@link TransitionWhitelist}/{@code ConditionEvaluator}，验证编排核心：归属/状态校验、
 * 白名单权威二次校验（1503 防跑偏）、stateChanges clamp、结局判定、recall/store 弱依赖降级。
 * 全程零外部依赖（无 DB/Nacos/Docker/LLM），任何机器与 CI 均可跑。
 */
class GameServiceTest {

    private static final long USER = 7L;
    private static final long SESSION = 100L;
    private static final long SCENARIO = 1L;
    private static final long START_NODE = 1000L;

    private GameSessionMapper sessionMapper;
    private GameStateMapper stateMapper;
    private TurnMapper turnMapper;
    private ScenarioClient scenarioClient;
    private AiEngineClient aiEngineClient;
    private MemoryClient memoryClient;
    private TurnPersister turnPersister;
    private GameService service;

    @BeforeEach
    void setup() {
        sessionMapper = mock(GameSessionMapper.class);
        stateMapper = mock(GameStateMapper.class);
        turnMapper = mock(TurnMapper.class);
        scenarioClient = mock(ScenarioClient.class);
        aiEngineClient = mock(AiEngineClient.class);
        memoryClient = mock(MemoryClient.class);
        turnPersister = mock(TurnPersister.class);
        service = new GameService(sessionMapper, stateMapper, turnMapper,
                scenarioClient, aiEngineClient, memoryClient,
                new TransitionWhitelist(), turnPersister);

        // persist 回填一个 Turn（事务落库本身在 TurnPersister 单测/集成覆盖，这里只验编排）
        when(turnPersister.persist(any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            Turn t = new Turn();
            t.setTurnNo(((GameSession) inv.getArgument(0)).getTurnCount() + 1);
            return t;
        });
        // recall/store 默认不显式打桩：Mockito 默认返回 null，被 recallSafely/storeSafely 安全吞掉。
    }

    // ============ 归属 / 状态校验（步骤①）============

    @Test
    void sessionNotFound_throws1220() {
        when(sessionMapper.selectById(SESSION)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.submitTurn(USER, SESSION, "进门"));
        assertEquals(1220, ex.getCode());
    }

    @Test
    void notOwner_throws1221() {
        when(sessionMapper.selectById(SESSION)).thenReturn(session(1, 999L)); // 别人的对局
        BizException ex = assertThrows(BizException.class, () -> service.submitTurn(USER, SESSION, "进门"));
        assertEquals(1221, ex.getCode());
    }

    @Test
    void notRunning_throws1202() {
        when(sessionMapper.selectById(SESSION)).thenReturn(session(2, USER)); // 已通关
        BizException ex = assertThrows(BizException.class, () -> service.submitTurn(USER, SESSION, "进门"));
        assertEquals(1202, ex.getCode());
    }

    // ============ 白名单二次校验（步骤⑤）============

    @Test
    void validTransition_advancesNodeAndStaysRunning() {
        wireRunning(List.of(trans(2000L, "always")));
        when(scenarioClient.getRunNode(SCENARIO, 2000L)).thenReturn(R.ok(node(2000L, false, null, List.of())));
        wireAi(ai(2000L, null));

        TurnResultVO r = service.submitTurn(USER, SESSION, "走进大厅");

        assertEquals(2000L, r.getState().getCurrentNodeId());
        assertFalse(r.isFinished());
        assertEquals(1, capturedStatus()); // STATUS_RUNNING
    }

    @Test
    void outOfWhitelistProposal_rejectedAndStays() {
        wireRunning(List.of(trans(2000L, "always")));
        wireAi(ai(9999L, null)); // 9999 不在白名单 → 防跑偏拒绝（1503），强制停留

        TurnResultVO r = service.submitTurn(USER, SESSION, "我要飞出这栋房子");

        assertEquals(START_NODE, r.getState().getCurrentNodeId()); // 没动
        assertFalse(r.isFinished());
        verify(scenarioClient, never()).getRunNode(SCENARIO, 9999L); // 越界目标不会被查询
    }

    @Test
    void conditionFalse_rejectedAndStays() {
        wireRunning(List.of(trans(3000L, "flag.has_key==true"))); // 需要 has_key，但当前没有
        wireAi(ai(3000L, null));

        TurnResultVO r = service.submitTurn(USER, SESSION, "推开那扇锁着的门");

        assertEquals(START_NODE, r.getState().getCurrentNodeId());
        assertFalse(r.isFinished());
    }

    @Test
    void conditionTrueAfterStateChange_transitionAccepted() {
        wireRunning(List.of(trans(3000L, "flag.has_key==true")));
        when(scenarioClient.getRunNode(SCENARIO, 3000L)).thenReturn(R.ok(node(3000L, false, null, List.of())));
        // 同一回合先 setFlag(has_key) 再跳转——白名单用「应用变更后」的 state 求值，应放行
        StateChanges sc = new StateChanges();
        sc.setSetFlags(List.of("has_key"));
        wireAi(ai(3000L, sc));

        TurnResultVO r = service.submitTurn(USER, SESSION, "捡起钥匙并开门");

        assertEquals(3000L, r.getState().getCurrentNodeId());
        assertTrue(r.getState().getFlags().get("has_key"));
    }

    // ============ 结局判定（步骤⑥）============

    @Test
    void transitionToWinEnding_finishesAsWin() {
        wireRunning(List.of(trans(9000L, "always")));
        when(scenarioClient.getRunNode(SCENARIO, 9000L)).thenReturn(R.ok(node(9000L, true, "WIN", List.of())));
        wireAi(ai(9000L, null));

        TurnResultVO r = service.submitTurn(USER, SESSION, "我指认管家就是凶手");

        assertTrue(r.isFinished());
        assertEquals(2, capturedStatus()); // STATUS_WIN
    }

    @Test
    void transitionToLoseEnding_finishesAsLose() {
        wireRunning(List.of(trans(9001L, "always")));
        when(scenarioClient.getRunNode(SCENARIO, 9001L)).thenReturn(R.ok(node(9001L, true, "LOSE", List.of())));
        wireAi(ai(9001L, null));

        TurnResultVO r = service.submitTurn(USER, SESSION, "我冲进黑暗");

        assertTrue(r.isFinished());
        assertEquals(3, capturedStatus()); // STATUS_LOSE
    }

    // ============ stateChanges 应用 + clamp ============

    @Test
    void stateChanges_appliedWithClamp() {
        wireRunning(List.of(trans(2000L, "always")));
        StateChanges sc = new StateChanges();
        sc.setSetFlags(List.of("found_body"));
        sc.setAddItems(List.of("生锈的钥匙"));
        sc.setAttrDelta(java.util.Map.of("sanity", 50, "evidence", -5)); // 80+50→clamp 100；0-5→clamp 0
        wireAi(ai(null, sc)); // 不跳转，专注验证 state

        TurnResultVO r = service.submitTurn(USER, SESSION, "搜查尸体");

        assertEquals(100, r.getState().getAttributes().get("sanity"), "sanity 上界 100");
        assertEquals(0, r.getState().getAttributes().get("evidence"), "属性下界 0，防 AI 写负");
        assertTrue(r.getState().getFlags().get("found_body"));
        assertTrue(r.getState().getInventory().contains("生锈的钥匙"));
        assertEquals(START_NODE, r.getState().getCurrentNodeId()); // proposeTo=null → 停留
    }

    // ============ AI 失败阻断 ============

    @Test
    void aiReturnsNullData_throws1500() {
        wireRunning(List.of(trans(2000L, "always")));
        when(aiEngineClient.generate(any())).thenReturn(R.<GenerateResponse>ok(null));
        BizException ex = assertThrows(BizException.class, () -> service.submitTurn(USER, SESSION, "动作"));
        assertEquals(1500, ex.getCode()); // AI_LLM_FAILED
    }

    @Test
    void aiClientThrows_degradesTo1901() {
        wireRunning(List.of(trans(2000L, "always")));
        when(aiEngineClient.generate(any())).thenThrow(new RuntimeException("connect timeout"));
        BizException ex = assertThrows(BizException.class, () -> service.submitTurn(USER, SESSION, "动作"));
        assertEquals(1901, ex.getCode()); // SERVICE_UNAVAILABLE
    }

    // ============ memory 弱依赖降级（步骤③/事务后 store）============

    @Test
    void recallFailure_doesNotBlockTurn() {
        wireRunning(List.of(trans(2000L, "always")));
        when(scenarioClient.getRunNode(SCENARIO, 2000L)).thenReturn(R.ok(node(2000L, false, null, List.of())));
        when(memoryClient.recall(any())).thenThrow(new RuntimeException("memory down"));
        wireAi(ai(2000L, null));

        TurnResultVO r = service.submitTurn(USER, SESSION, "继续探索");

        assertEquals(2000L, r.getState().getCurrentNodeId()); // 召回失败仍正常推进
        assertFalse(r.isFinished());
    }

    @Test
    void storeFailure_doesNotBlockTurn() {
        wireRunning(List.of(trans(2000L, "always")));
        when(scenarioClient.getRunNode(SCENARIO, 2000L)).thenReturn(R.ok(node(2000L, false, null, List.of())));
        MemoryItem item = new MemoryItem();
        item.setContent("玩家进入大厅");
        item.setMemType("event");
        item.setImportance(3);
        GenerateResponse g = ai(2000L, null);
        g.setMemoryToStore(List.of(item));
        wireAi(g);
        when(memoryClient.store(any())).thenThrow(new RuntimeException("store down"));

        TurnResultVO r = service.submitTurn(USER, SESSION, "进入大厅");

        assertEquals(2000L, r.getState().getCurrentNodeId()); // 事务后 store 失败仅 warn，不回滚
        assertFalse(r.isFinished());
    }

    // ============ fixtures ============

    private void wireRunning(List<TransitionDTO> transitions) {
        when(sessionMapper.selectById(SESSION)).thenReturn(session(1, USER));
        when(stateMapper.selectBySessionId(SESSION)).thenReturn(state());
        when(scenarioClient.getRunNode(SCENARIO, START_NODE))
                .thenReturn(R.ok(node(START_NODE, false, null, transitions)));
        when(scenarioClient.getRunScenario(SCENARIO)).thenReturn(R.ok(scenario()));
    }

    private void wireAi(GenerateResponse g) {
        when(aiEngineClient.generate(any())).thenReturn(R.ok(g));
    }

    private int capturedStatus() {
        ArgumentCaptor<Integer> cap = ArgumentCaptor.forClass(Integer.class);
        verify(turnPersister).persist(any(), any(), any(), any(), cap.capture());
        return cap.getValue();
    }

    private GameSession session(int status, long owner) {
        GameSession s = new GameSession();
        s.setId(SESSION);
        s.setUserId(owner);
        s.setScenarioId(SCENARIO);
        s.setStatus(status);
        s.setTurnCount(3);
        return s;
    }

    private GameState state() {
        GameState st = new GameState();
        st.setId(1L);
        st.setSessionId(SESSION);
        st.setCurrentNodeId(START_NODE);
        st.setFlags("{}");
        st.setInventory("[]");
        st.setAttributes("{\"sanity\":80,\"evidence\":0}");
        st.setRecentSummary("");
        return st;
    }

    private SceneNodeRunVO node(long id, boolean ending, String endingType, List<TransitionDTO> trans) {
        SceneNodeRunVO n = new SceneNodeRunVO();
        n.setId(id);
        n.setNodeKey("node_" + id);
        n.setTitle("节点" + id);
        n.setNarrativeBrief("简述");
        n.setIsEnding(ending);
        n.setEndingType(endingType);
        n.setTransitions(trans);
        n.setNpcs(List.of());
        return n;
    }

    private TransitionDTO trans(long to, String cond) {
        TransitionDTO t = new TransitionDTO();
        t.setToNodeId(to);
        t.setCondition(cond);
        t.setPriority(10);
        t.setDescription("前往" + to);
        return t;
    }

    private ScenarioRunVO scenario() {
        ScenarioRunVO sc = new ScenarioRunVO();
        sc.setId(SCENARIO);
        sc.setTitle("迷雾古宅");
        sc.setGenre("mystery");
        sc.setStartNodeId(START_NODE);
        return sc;
    }

    private GenerateResponse ai(Long proposeTo, StateChanges changes) {
        GenerateResponse g = new GenerateResponse();
        g.setNarrative("门吱呀一声开了。");
        g.setNpcDialogues(List.of());
        g.setStateChanges(changes);
        if (proposeTo != null) {
            ProposedTransition p = new ProposedTransition();
            p.setToNodeId(proposeTo);
            g.setProposedTransition(p);
        }
        return g;
    }
}
