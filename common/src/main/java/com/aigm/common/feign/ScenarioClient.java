package com.aigm.common.feign;

import com.aigm.common.feign.dto.SceneNodeRunVO;
import com.aigm.common.feign.dto.ScenarioRunVO;
import com.aigm.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** scenario-service 运行时只读接口（INTERNAL，基线 §5.2 /api/scenario/run/**）。 */
@FeignClient(name = "scenario-service", path = "/api/scenario/run")
public interface ScenarioClient {

    @GetMapping("/{scenarioId}")
    R<ScenarioRunVO> getRunScenario(@PathVariable("scenarioId") Long scenarioId);

    @GetMapping("/{scenarioId}/node/{nodeId}")
    R<SceneNodeRunVO> getRunNode(@PathVariable("scenarioId") Long scenarioId,
                                 @PathVariable("nodeId") Long nodeId);
}
