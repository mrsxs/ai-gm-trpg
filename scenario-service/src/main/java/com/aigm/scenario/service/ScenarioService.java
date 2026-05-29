package com.aigm.scenario.service;

import com.aigm.common.condition.ConditionEvaluator;
import com.aigm.common.constant.RoleConst;
import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import com.aigm.common.util.PageQuery;
import com.aigm.common.util.PageResult;
import com.aigm.common.web.UserContext;
import com.aigm.scenario.dto.*;
import com.aigm.scenario.entity.*;
import com.aigm.scenario.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 剧本/节点/NPC/分支 CRUD + 状态机建模 + 发布结构校验。
 * RBAC 信网关头：列表/详情/查 = 任意登录；写操作 = 本人 AUTHOR 或 ADMIN。
 */
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioMapper scenarioMapper;
    private final SceneNodeMapper nodeMapper;
    private final NpcMapper npcMapper;
    private final NodeNpcMapper nodeNpcMapper;
    private final TransitionMapper transitionMapper;

    // ============================ 剧本 ============================

    public PageResult<ScenarioVO> listPublished(Long page, Long size, String genre) {
        PageQuery pq = PageQuery.of(page, size);
        LambdaQueryWrapper<Scenario> w = new LambdaQueryWrapper<Scenario>()
                .eq(Scenario::getStatus, 1)
                .eq(StringUtils.hasText(genre), Scenario::getGenre, genre)
                .orderByDesc(Scenario::getUpdatedAt);
        IPage<Scenario> p = scenarioMapper.selectPage(new Page<>(pq.getPage(), pq.getSize()), w);
        return toScenarioPage(p);
    }

    public PageResult<ScenarioVO> listMine(Long page, Long size, Integer status) {
        UserContext.requireAnyRole(RoleConst.AUTHOR, RoleConst.ADMIN);
        Long uid = UserContext.requireUserId();
        PageQuery pq = PageQuery.of(page, size);
        LambdaQueryWrapper<Scenario> w = new LambdaQueryWrapper<Scenario>()
                .eq(Scenario::getAuthorId, uid)
                .eq(status != null, Scenario::getStatus, status)
                .orderByDesc(Scenario::getUpdatedAt);
        IPage<Scenario> p = scenarioMapper.selectPage(new Page<>(pq.getPage(), pq.getSize()), w);
        return toScenarioPage(p);
    }

    public ScenarioDetailVO detail(Long id) {
        Scenario s = requireScenario(id);
        ScenarioDetailVO vo = new ScenarioDetailVO();
        vo.setScenario(toScenarioVO(s));

        List<SceneNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<SceneNode>()
                .eq(SceneNode::getScenarioId, id).orderByAsc(SceneNode::getSortNo).orderByAsc(SceneNode::getId));
        List<Npc> npcs = npcMapper.selectList(new LambdaQueryWrapper<Npc>()
                .eq(Npc::getScenarioId, id).orderByAsc(Npc::getId));
        List<Transition> trans = transitionMapper.selectList(new LambdaQueryWrapper<Transition>()
                .eq(Transition::getScenarioId, id).orderByDesc(Transition::getPriority).orderByAsc(Transition::getId));

        Map<Long, List<Long>> nodeNpcMap = nodeNpcMapForNodes(
                nodes.stream().map(SceneNode::getId).collect(Collectors.toList()));

        vo.setNodes(nodes.stream().map(n -> toNodeVO(n, nodeNpcMap.getOrDefault(n.getId(), List.of()))).collect(Collectors.toList()));
        vo.setNpcs(npcs.stream().map(this::toNpcVO).collect(Collectors.toList()));
        vo.setTransitions(trans.stream().map(this::toTransitionVO).collect(Collectors.toList()));
        return vo;
    }

    @Transactional
    public Long createScenario(ScenarioCreateDTO dto) {
        UserContext.requireAnyRole(RoleConst.AUTHOR, RoleConst.ADMIN);
        Long uid = UserContext.requireUserId();
        Scenario s = new Scenario();
        s.setTitle(dto.getTitle());
        s.setIntro(dto.getIntro());
        s.setGenre(dto.getGenre());
        s.setCover(dto.getCover());
        s.setStatus(0);
        s.setAuthorId(uid);
        scenarioMapper.insert(s);
        return s.getId();
    }

    @Transactional
    public void updateScenario(Long id, ScenarioUpdateDTO dto) {
        Scenario s = requireOwned(id);
        if (dto.getTitle() != null) s.setTitle(dto.getTitle());
        if (dto.getIntro() != null) s.setIntro(dto.getIntro());
        if (dto.getGenre() != null) s.setGenre(dto.getGenre());
        if (dto.getCover() != null) s.setCover(dto.getCover());
        if (dto.getStartNodeId() != null) {
            requireNodeOfScenario(dto.getStartNodeId(), id);
            s.setStartNodeId(dto.getStartNodeId());
        }
        scenarioMapper.updateById(s);
    }

    @Transactional
    public void publish(Long id, Integer status) {
        Scenario s = requireOwned(id);
        if (status != null && status == 1) {
            validateStructure(s);
        }
        s.setStatus(status);
        scenarioMapper.updateById(s);
    }

    @Transactional
    public void deleteScenario(Long id) {
        requireOwned(id);
        scenarioMapper.deleteById(id);
    }

    // ============================ 节点 ============================

    public List<SceneNodeVO> listNodes(Long scenarioId) {
        requireScenario(scenarioId);
        List<SceneNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<SceneNode>()
                .eq(SceneNode::getScenarioId, scenarioId).orderByAsc(SceneNode::getSortNo).orderByAsc(SceneNode::getId));
        Map<Long, List<Long>> nodeNpcMap = nodeNpcMapForNodes(
                nodes.stream().map(SceneNode::getId).collect(Collectors.toList()));
        return nodes.stream().map(n -> toNodeVO(n, nodeNpcMap.getOrDefault(n.getId(), List.of()))).collect(Collectors.toList());
    }

    @Transactional
    public Long createNode(Long scenarioId, NodeCreateDTO dto) {
        requireOwned(scenarioId);
        SceneNode n = new SceneNode();
        n.setScenarioId(scenarioId);
        n.setNodeKey(dto.getNodeKey());
        n.setTitle(dto.getTitle());
        n.setNarrativeBrief(dto.getNarrativeBrief());
        n.setIsEnding(dto.getIsEnding() == null ? 0 : dto.getIsEnding());
        n.setEndingType(dto.getEndingType());
        n.setSortNo(0);
        nodeMapper.insert(n);
        replaceNodeNpcs(n.getId(), dto.getNpcIds());
        return n.getId();
    }

    @Transactional
    public void updateNode(Long nodeId, NodeUpdateDTO dto) {
        SceneNode n = nodeMapper.selectById(nodeId);
        if (n == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "节点不存在");
        requireOwned(n.getScenarioId());
        if (dto.getTitle() != null) n.setTitle(dto.getTitle());
        if (dto.getNarrativeBrief() != null) n.setNarrativeBrief(dto.getNarrativeBrief());
        if (dto.getIsEnding() != null) n.setIsEnding(dto.getIsEnding());
        if (dto.getEndingType() != null) n.setEndingType(dto.getEndingType());
        nodeMapper.updateById(n);
        if (dto.getNpcIds() != null) {
            replaceNodeNpcs(nodeId, dto.getNpcIds());
        }
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        SceneNode n = nodeMapper.selectById(nodeId);
        if (n == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "节点不存在");
        requireOwned(n.getScenarioId());
        // 级联删相关 transition（出边+入边）与 node_npc 关联
        transitionMapper.delete(new LambdaQueryWrapper<Transition>()
                .eq(Transition::getFromNodeId, nodeId).or().eq(Transition::getToNodeId, nodeId));
        nodeNpcMapper.delete(new LambdaQueryWrapper<NodeNpc>().eq(NodeNpc::getNodeId, nodeId));
        nodeMapper.deleteById(nodeId);
    }

    // ============================ NPC ============================

    public List<NpcVO> listNpcs(Long scenarioId) {
        requireScenario(scenarioId);
        return npcMapper.selectList(new LambdaQueryWrapper<Npc>()
                        .eq(Npc::getScenarioId, scenarioId).orderByAsc(Npc::getId))
                .stream().map(this::toNpcVO).collect(Collectors.toList());
    }

    @Transactional
    public Long createNpc(Long scenarioId, NpcCreateDTO dto) {
        requireOwned(scenarioId);
        Npc npc = new Npc();
        npc.setScenarioId(scenarioId);
        npc.setNpcKey(dto.getNpcKey());
        npc.setName(dto.getName());
        npc.setPersona(dto.getPersona());
        npc.setBackground(dto.getBackground());
        npc.setSecret(dto.getSecret());
        npc.setAvatar(dto.getAvatar());
        npcMapper.insert(npc);
        return npc.getId();
    }

    @Transactional
    public void updateNpc(Long npcId, NpcUpdateDTO dto) {
        Npc npc = npcMapper.selectById(npcId);
        if (npc == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "NPC不存在");
        requireOwned(npc.getScenarioId());
        if (dto.getName() != null) npc.setName(dto.getName());
        if (dto.getPersona() != null) npc.setPersona(dto.getPersona());
        if (dto.getBackground() != null) npc.setBackground(dto.getBackground());
        if (dto.getSecret() != null) npc.setSecret(dto.getSecret());
        if (dto.getAvatar() != null) npc.setAvatar(dto.getAvatar());
        npcMapper.updateById(npc);
    }

    @Transactional
    public void deleteNpc(Long npcId) {
        Npc npc = npcMapper.selectById(npcId);
        if (npc == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "NPC不存在");
        requireOwned(npc.getScenarioId());
        nodeNpcMapper.delete(new LambdaQueryWrapper<NodeNpc>().eq(NodeNpc::getNpcId, npcId));
        npcMapper.deleteById(npcId);
    }

    // ============================ 分支 ============================

    public List<TransitionVO> listTransitions(Long scenarioId) {
        requireScenario(scenarioId);
        return transitionMapper.selectList(new LambdaQueryWrapper<Transition>()
                        .eq(Transition::getScenarioId, scenarioId)
                        .orderByDesc(Transition::getPriority).orderByAsc(Transition::getId))
                .stream().map(this::toTransitionVO).collect(Collectors.toList());
    }

    @Transactional
    public Long createTransition(TransitionCreateDTO dto) {
        requireOwned(dto.getScenarioId());
        requireNodeOfScenario(dto.getFromNodeId(), dto.getScenarioId());
        requireNodeOfScenario(dto.getToNodeId(), dto.getScenarioId());
        if (!ConditionEvaluator.isValidSyntax(dto.getConditionExpr())) {
            throw new BizException(ResultCode.PARAM_INVALID, "非法condition");
        }
        Transition t = new Transition();
        t.setScenarioId(dto.getScenarioId());
        t.setFromNodeId(dto.getFromNodeId());
        t.setToNodeId(dto.getToNodeId());
        t.setConditionExpr(dto.getConditionExpr());
        t.setDescription(dto.getDescription());
        t.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        transitionMapper.insert(t);
        return t.getId();
    }

    @Transactional
    public void updateTransition(Long id, TransitionUpdateDTO dto) {
        Transition t = transitionMapper.selectById(id);
        if (t == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "分支不存在");
        requireOwned(t.getScenarioId());
        if (dto.getToNodeId() != null) {
            requireNodeOfScenario(dto.getToNodeId(), t.getScenarioId());
            t.setToNodeId(dto.getToNodeId());
        }
        if (dto.getConditionExpr() != null) {
            if (!ConditionEvaluator.isValidSyntax(dto.getConditionExpr())) {
                throw new BizException(ResultCode.PARAM_INVALID, "非法condition");
            }
            t.setConditionExpr(dto.getConditionExpr());
        }
        if (dto.getDescription() != null) t.setDescription(dto.getDescription());
        if (dto.getPriority() != null) t.setPriority(dto.getPriority());
        transitionMapper.updateById(t);
    }

    @Transactional
    public void deleteTransition(Long id) {
        Transition t = transitionMapper.selectById(id);
        if (t == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "分支不存在");
        requireOwned(t.getScenarioId());
        transitionMapper.deleteById(id);
    }

    // ============================ 发布结构校验 ============================

    private void validateStructure(Scenario s) {
        Long sid = s.getId();
        List<SceneNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<SceneNode>()
                .eq(SceneNode::getScenarioId, sid));
        Set<Long> nodeIds = nodes.stream().map(SceneNode::getId).collect(Collectors.toSet());

        // 1. 起始节点存在且属本剧本
        if (s.getStartNodeId() == null || !nodeIds.contains(s.getStartNodeId())) {
            throw new BizException(ResultCode.SCENARIO_NO_START_NODE);
        }

        List<Transition> trans = transitionMapper.selectList(new LambdaQueryWrapper<Transition>()
                .eq(Transition::getScenarioId, sid));

        // 4. 所有 transition 两端节点存在且属本剧本、condition 合法
        for (Transition t : trans) {
            if (!nodeIds.contains(t.getFromNodeId()) || !nodeIds.contains(t.getToNodeId())) {
                throw new BizException(ResultCode.STATE_INVALID, "存在两端节点不属于本剧本的分支");
            }
            if (!ConditionEvaluator.isValidSyntax(t.getConditionExpr())) {
                throw new BizException(ResultCode.PARAM_INVALID, "存在非法condition的分支");
            }
        }

        // 2. 每个非结局节点至少一条出边
        Set<Long> fromNodes = trans.stream().map(Transition::getFromNodeId).collect(Collectors.toSet());
        for (SceneNode n : nodes) {
            boolean ending = n.getIsEnding() != null && n.getIsEnding() == 1;
            if (!ending && !fromNodes.contains(n.getId())) {
                throw new BizException(ResultCode.NODE_NO_TRANSITION);
            }
        }

        // 3. 至少 1 个结局节点
        boolean hasEnding = nodes.stream().anyMatch(n -> n.getIsEnding() != null && n.getIsEnding() == 1);
        if (!hasEnding) {
            throw new BizException(ResultCode.STATE_INVALID, "至少需要一个结局节点");
        }

        // 5. 从 start BFS 沿 transition 能到达某结局节点
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Transition t : trans) {
            adj.computeIfAbsent(t.getFromNodeId(), k -> new ArrayList<>()).add(t.getToNodeId());
        }
        Set<Long> endingIds = nodes.stream()
                .filter(n -> n.getIsEnding() != null && n.getIsEnding() == 1)
                .map(SceneNode::getId).collect(Collectors.toSet());
        if (!canReachEnding(s.getStartNodeId(), adj, endingIds)) {
            throw new BizException(ResultCode.STATE_INVALID, "状态机无法从起点到达任何结局");
        }
    }

    private boolean canReachEnding(Long start, Map<Long, List<Long>> adj, Set<Long> endingIds) {
        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            if (endingIds.contains(cur)) return true;
            for (Long next : adj.getOrDefault(cur, List.of())) {
                if (visited.add(next)) queue.add(next);
            }
        }
        return false;
    }

    // ============================ 工具 ============================

    private Scenario requireScenario(Long id) {
        Scenario s = scenarioMapper.selectById(id);
        if (s == null) throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "剧本不存在");
        return s;
    }

    /** 写操作鉴权：ADMIN 任意，AUTHOR 仅本人。 */
    private Scenario requireOwned(Long id) {
        Scenario s = requireScenario(id);
        Long uid = UserContext.requireUserId();
        if (UserContext.hasRole(RoleConst.ADMIN)) return s;
        if (UserContext.hasRole(RoleConst.AUTHOR) && Objects.equals(s.getAuthorId(), uid)) return s;
        throw new BizException(ResultCode.AUTH_NO_PERMISSION);
    }

    private void requireNodeOfScenario(Long nodeId, Long scenarioId) {
        SceneNode n = nodeMapper.selectById(nodeId);
        if (n == null || !Objects.equals(n.getScenarioId(), scenarioId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "节点不存在或不属于该剧本");
        }
    }

    private void replaceNodeNpcs(Long nodeId, List<Long> npcIds) {
        nodeNpcMapper.delete(new LambdaQueryWrapper<NodeNpc>().eq(NodeNpc::getNodeId, nodeId));
        if (npcIds == null || npcIds.isEmpty()) return;
        for (Long npcId : new LinkedHashSet<>(npcIds)) {
            NodeNpc nn = new NodeNpc();
            nn.setNodeId(nodeId);
            nn.setNpcId(npcId);
            nodeNpcMapper.insert(nn);
        }
    }

    private Map<Long, List<Long>> nodeNpcMapForNodes(List<Long> nodeIds) {
        Map<Long, List<Long>> map = new HashMap<>();
        if (nodeIds == null || nodeIds.isEmpty()) return map;
        List<NodeNpc> links = nodeNpcMapper.selectList(new LambdaQueryWrapper<NodeNpc>()
                .in(NodeNpc::getNodeId, nodeIds));
        for (NodeNpc nn : links) {
            map.computeIfAbsent(nn.getNodeId(), k -> new ArrayList<>()).add(nn.getNpcId());
        }
        return map;
    }

    private PageResult<ScenarioVO> toScenarioPage(IPage<Scenario> p) {
        List<ScenarioVO> list = p.getRecords().stream().map(this::toScenarioVO).collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    private ScenarioVO toScenarioVO(Scenario s) {
        ScenarioVO vo = new ScenarioVO();
        BeanUtils.copyProperties(s, vo);
        return vo;
    }

    private SceneNodeVO toNodeVO(SceneNode n, List<Long> npcIds) {
        SceneNodeVO vo = new SceneNodeVO();
        BeanUtils.copyProperties(n, vo);
        vo.setNpcIds(npcIds);
        return vo;
    }

    private NpcVO toNpcVO(Npc n) {
        NpcVO vo = new NpcVO();
        BeanUtils.copyProperties(n, vo);
        return vo;
    }

    private TransitionVO toTransitionVO(Transition t) {
        TransitionVO vo = new TransitionVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }
}
