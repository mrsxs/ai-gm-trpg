package com.aigm.common.feign;

import com.aigm.common.feign.dto.RecallRequest;
import com.aigm.common.feign.dto.RecallResult;
import com.aigm.common.feign.dto.StoreRequest;
import com.aigm.common.feign.dto.StoreResult;
import com.aigm.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** memory-service（INTERNAL，基线 §5.5）。 */
@FeignClient(name = "memory-service", path = "/api/memory")
public interface MemoryClient {

    @PostMapping("/store")
    R<StoreResult> store(@RequestBody StoreRequest req);

    @PostMapping("/recall")
    R<RecallResult> recall(@RequestBody RecallRequest req);
}
