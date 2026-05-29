package com.aigm.game.service;

import com.aigm.common.condition.ConditionEvaluator;
import com.aigm.common.feign.dto.ProposedTransition;
import com.aigm.common.feign.dto.SceneNodeRunVO;
import com.aigm.common.feign.dto.TransitionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 白名单权威二次校验（基线 §6.5 第3条）：用应用 stateChanges 后的权威 state 再校验一次，
 * 防御性——下游不信任上游。不合法/越界返回 null（强制停留），记 1503。
 */
@Slf4j
@Component
public class TransitionWhitelist {

    /** 返回最终生效的 toNodeId；越界或 condition 不满足 → null（停留）。 */
    public Long resolve(SceneNodeRunVO node,
                        ProposedTransition proposed,
                        Map<String, Boolean> flags,
                        Map<String, Integer> attributes,
                        List<String> inventory) {
        if (proposed == null || proposed.getToNodeId() == null) {
            return null; // 合法停留
        }
        Long to = proposed.getToNodeId();
        List<TransitionDTO> transitions = node.getTransitions();
        if (transitions == null) {
            log.warn("[1503] node {} 无 transitions，拒绝跳转 toNodeId={}", node.getId(), to);
            return null;
        }
        Optional<TransitionDTO> matched = transitions.stream()
                .filter(t -> Objects.equals(t.getToNodeId(), to))
                .findFirst();
        if (matched.isEmpty()) {
            log.warn("[1503] toNodeId {} 不在节点 {} 的白名单内，拒绝", to, node.getId());
            return null;
        }
        String cond = matched.get().getCondition();
        if (!ConditionEvaluator.evaluate(cond, flags, attributes, inventory)) {
            log.warn("[1503] condition '{}' 在应用变更后求值为假，拒绝跳转 toNodeId={}", cond, to);
            return null;
        }
        return to;
    }
}
