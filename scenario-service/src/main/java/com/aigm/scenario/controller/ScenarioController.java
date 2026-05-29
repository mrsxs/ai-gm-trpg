package com.aigm.scenario.controller;

import com.aigm.common.result.R;
import com.aigm.common.util.PageResult;
import com.aigm.scenario.dto.*;
import com.aigm.scenario.service.ScenarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 剧本/节点/NPC/分支 对外 CRUD（基线 §5.2）。经网关 /api/scenario/**。 */
@RestController
@RequestMapping("/api/scenario")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    // ===== 剧本 =====

    @GetMapping("/published")
    public R<PageResult<ScenarioVO>> published(@RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size,
                                               @RequestParam(required = false) String genre) {
        return R.ok(scenarioService.listPublished(page, size, genre));
    }

    @GetMapping("/mine")
    public R<PageResult<ScenarioVO>> mine(@RequestParam(required = false) Long page,
                                          @RequestParam(required = false) Long size,
                                          @RequestParam(required = false) Integer status) {
        return R.ok(scenarioService.listMine(page, size, status));
    }

    @GetMapping("/{id}")
    public R<ScenarioDetailVO> detail(@PathVariable Long id) {
        return R.ok(scenarioService.detail(id));
    }

    @PostMapping
    public R<IdVO> create(@RequestBody @Valid ScenarioCreateDTO dto) {
        return R.ok(new IdVO(scenarioService.createScenario(dto)));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody ScenarioUpdateDTO dto) {
        scenarioService.updateScenario(id, dto);
        return R.ok(true);
    }

    @PutMapping("/{id}/publish")
    public R<Boolean> publish(@PathVariable Long id, @RequestBody @Valid PublishDTO dto) {
        scenarioService.publish(id, dto.getStatus());
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        scenarioService.deleteScenario(id);
        return R.ok(true);
    }

    // ===== 节点 =====

    @GetMapping("/{sid}/nodes")
    public R<List<SceneNodeVO>> listNodes(@PathVariable Long sid) {
        return R.ok(scenarioService.listNodes(sid));
    }

    @PostMapping("/{sid}/nodes")
    public R<IdVO> createNode(@PathVariable Long sid, @RequestBody @Valid NodeCreateDTO dto) {
        return R.ok(new IdVO(scenarioService.createNode(sid, dto)));
    }

    @PutMapping("/nodes/{nodeId}")
    public R<Boolean> updateNode(@PathVariable Long nodeId, @RequestBody NodeUpdateDTO dto) {
        scenarioService.updateNode(nodeId, dto);
        return R.ok(true);
    }

    @DeleteMapping("/nodes/{nodeId}")
    public R<Boolean> deleteNode(@PathVariable Long nodeId) {
        scenarioService.deleteNode(nodeId);
        return R.ok(true);
    }

    // ===== NPC =====

    @GetMapping("/{sid}/npcs")
    public R<List<NpcVO>> listNpcs(@PathVariable Long sid) {
        return R.ok(scenarioService.listNpcs(sid));
    }

    @PostMapping("/{sid}/npcs")
    public R<IdVO> createNpc(@PathVariable Long sid, @RequestBody @Valid NpcCreateDTO dto) {
        return R.ok(new IdVO(scenarioService.createNpc(sid, dto)));
    }

    @PutMapping("/npcs/{npcId}")
    public R<Boolean> updateNpc(@PathVariable Long npcId, @RequestBody NpcUpdateDTO dto) {
        scenarioService.updateNpc(npcId, dto);
        return R.ok(true);
    }

    @DeleteMapping("/npcs/{npcId}")
    public R<Boolean> deleteNpc(@PathVariable Long npcId) {
        scenarioService.deleteNpc(npcId);
        return R.ok(true);
    }

    // ===== 分支 =====

    @GetMapping("/{sid}/transitions")
    public R<List<TransitionVO>> listTransitions(@PathVariable Long sid) {
        return R.ok(scenarioService.listTransitions(sid));
    }

    @PostMapping("/transitions")
    public R<IdVO> createTransition(@RequestBody @Valid TransitionCreateDTO dto) {
        return R.ok(new IdVO(scenarioService.createTransition(dto)));
    }

    @PutMapping("/transitions/{id}")
    public R<Boolean> updateTransition(@PathVariable Long id, @RequestBody TransitionUpdateDTO dto) {
        scenarioService.updateTransition(id, dto);
        return R.ok(true);
    }

    @DeleteMapping("/transitions/{id}")
    public R<Boolean> deleteTransition(@PathVariable Long id) {
        scenarioService.deleteTransition(id);
        return R.ok(true);
    }
}
