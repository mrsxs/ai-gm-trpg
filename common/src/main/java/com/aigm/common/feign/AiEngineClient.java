package com.aigm.common.feign;

import com.aigm.common.feign.dto.GenerateRequest;
import com.aigm.common.feign.dto.GenerateResponse;
import com.aigm.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** ai-engine-service（INTERNAL，基线 §5.4）。 */
@FeignClient(name = "ai-engine-service", path = "/api/ai")
public interface AiEngineClient {

    @PostMapping("/generate")
    R<GenerateResponse> generate(@RequestBody GenerateRequest req);
}
