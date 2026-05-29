package com.aigm.game.service;

import com.aigm.common.feign.dto.ProposedTransition;
import com.aigm.common.feign.dto.SceneNodeRunVO;
import com.aigm.common.feign.dto.TransitionDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 白名单权威二次校验（基线 §6.5，防跑偏 1503）单测。 */
class TransitionWhitelistTest {

    private final TransitionWhitelist whitelist = new TransitionWhitelist();

    private SceneNodeRunVO node() {
        SceneNodeRunVO n = new SceneNodeRunVO();
        n.setId(3002L);
        TransitionDTO t1 = new TransitionDTO();
        t1.setToNodeId(3003L);
        t1.setCondition("always");
        t1.setPriority(10);
        TransitionDTO t2 = new TransitionDTO();
        t2.setToNodeId(3004L);
        t2.setCondition("flag.has_key==true");
        t2.setPriority(20);
        n.setTransitions(List.of(t1, t2));
        return n;
    }

    private ProposedTransition proposed(Long to) {
        ProposedTransition p = new ProposedTransition();
        p.setToNodeId(to);
        return p;
    }

    @Test
    void nullProposedMeansStay() {
        assertNull(whitelist.resolve(node(), null, Map.of(), Map.of(), List.of()));
        assertNull(whitelist.resolve(node(), proposed(null), Map.of(), Map.of(), List.of()));
    }

    @Test
    void validAlwaysTransitionAccepted() {
        assertEquals(3003L, whitelist.resolve(node(), proposed(3003L), Map.of(), Map.of(), List.of()));
    }

    @Test
    void conditionalTransitionRejectedWhenConditionFalse() {
        // has_key 缺省 false → flag.has_key==true 不满足 → 拒绝（1503）
        assertNull(whitelist.resolve(node(), proposed(3004L), Map.of(), Map.of(), List.of()));
    }

    @Test
    void conditionalTransitionAcceptedWhenConditionTrue() {
        assertEquals(3004L, whitelist.resolve(node(), proposed(3004L),
                Map.of("has_key", true), Map.of(), List.of()));
    }

    @Test
    void outOfWhitelistRejected() {
        // 3099 不在当前节点白名单 → 防跑偏拒绝（1503），AI 跳不出轨道
        assertNull(whitelist.resolve(node(), proposed(3099L), Map.of("has_key", true), Map.of(), List.of()));
    }
}
