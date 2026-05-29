package com.aigm.scenario.service;

import com.aigm.common.exception.BizException;
import com.aigm.common.feign.dto.NpcRunVO;
import com.aigm.common.feign.dto.SceneNodeRunVO;
import com.aigm.common.feign.dto.ScenarioRunVO;
import com.aigm.common.feign.dto.TransitionDTO;
import com.aigm.common.result.ResultCode;
import com.aigm.scenario.entity.NodeNpc;
import com.aigm.scenario.entity.Npc;
import com.aigm.scenario.entity.SceneNode;
import com.aigm.scenario.entity.Scenario;
import com.aigm.scenario.entity.Transition;
import com.aigm.scenario.mapper.NodeNpcMapper;
import com.aigm.scenario.mapper.NpcMapper;
import com.aigm.scenario.mapper.SceneNodeMapper;
import com.aigm.scenario.mapper.ScenarioMapper;
import com.aigm.scenario.mapper.TransitionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 运行时只读：供 game-service 经 Feign 调用，按当前节点/白名单组织剧本结构。
 * 返回类型一律用 common 已有 DTO（ScenarioRunVO/SceneNodeRunVO/NpcRunVO/TransitionDTO）。
 */
@Service
@RequiredArgsConstructor
public class ScenarioRunService {

    private final ScenarioMapper scenarioMapper;
    private final SceneNodeMapper nodeMapper;
    private final NpcMapper npcMapper;
    private final NodeNpcMapper nodeNpcMapper;
    private final TransitionMapper transitionMapper;

    public ScenarioRunVO runScenario(Long scenarioId) {
        Scenario s = scenarioMapper.selectById(scenarioId);
        if (s == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "剧本不存在");
        ScenarioRunVO vo = new ScenarioRunVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setGenre(s.getGenre());
        vo.setStartNodeId(s.getStartNodeId());
        return vo;
    }

    public SceneNodeRunVO runNode(Long scenarioId, Long nodeId) {
        SceneNode n = nodeMapper.selectById(nodeId);
        if (n == null || !Objects.equals(n.getScenarioId(), scenarioId)) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "节点不存在或不属于该剧本");
        }
        SceneNodeRunVO vo = new SceneNodeRunVO();
        vo.setId(n.getId());
        vo.setNodeKey(n.getNodeKey());
        vo.setTitle(n.getTitle());
        vo.setNarrativeBrief(n.getNarrativeBrief());
        vo.setIsEnding(n.getIsEnding() != null && n.getIsEnding() == 1);
        vo.setEndingType(n.getEndingType());
        vo.setNpcs(loadNodeNpcs(nodeId));
        vo.setTransitions(loadTransitions(nodeId));
        return vo;
    }

    private List<NpcRunVO> loadNodeNpcs(Long nodeId) {
        List<NodeNpc> links = nodeNpcMapper.selectList(new LambdaQueryWrapper<NodeNpc>()
                .eq(NodeNpc::getNodeId, nodeId));
        List<Long> npcIds = links.stream().map(NodeNpc::getNpcId).collect(Collectors.toList());
        if (npcIds.isEmpty()) return List.of();
        return npcMapper.selectBatchIds(npcIds).stream().map(this::toNpcRunVO).collect(Collectors.toList());
    }

    private NpcRunVO toNpcRunVO(Npc npc) {
        NpcRunVO vo = new NpcRunVO();
        vo.setNpcId(npc.getId());
        vo.setNpcKey(npc.getNpcKey());
        vo.setName(npc.getName());
        vo.setPersona(npc.getPersona());
        vo.setBackground(npc.getBackground());
        vo.setSecret(npc.getSecret());
        return vo;
    }

    private List<TransitionDTO> loadTransitions(Long nodeId) {
        List<Transition> trans = transitionMapper.selectList(new LambdaQueryWrapper<Transition>()
                .eq(Transition::getFromNodeId, nodeId)
                .orderByDesc(Transition::getPriority).orderByAsc(Transition::getId));
        return trans.stream().map(t -> {
            TransitionDTO dto = new TransitionDTO();
            dto.setToNodeId(t.getToNodeId());
            dto.setCondition(t.getConditionExpr());
            dto.setPriority(t.getPriority());
            dto.setDescription(t.getDescription());
            return dto;
        }).collect(Collectors.toList());
    }
}
