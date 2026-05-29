package com.aigm.scenario.controller;

import com.aigm.common.feign.dto.SceneNodeRunVO;
import com.aigm.common.feign.dto.ScenarioRunVO;
import com.aigm.common.result.R;
import com.aigm.scenario.service.ScenarioRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 运行时只读接口（INTERNAL，供 game-service Feign 调用）。
 * 路径属 /api/scenario/run/**，网关不对外路由，不需鉴权注解。
 */
@RestController
@RequestMapping("/api/scenario/run")
@RequiredArgsConstructor
public class ScenarioRunController {

    private final ScenarioRunService scenarioRunService;

    @GetMapping("/{scenarioId}")
    public R<ScenarioRunVO> runScenario(@PathVariable Long scenarioId) {
        return R.ok(scenarioRunService.runScenario(scenarioId));
    }

    @GetMapping("/{scenarioId}/node/{nodeId}")
    public R<SceneNodeRunVO> runNode(@PathVariable Long scenarioId, @PathVariable Long nodeId) {
        return R.ok(scenarioRunService.runNode(scenarioId, nodeId));
    }
}
