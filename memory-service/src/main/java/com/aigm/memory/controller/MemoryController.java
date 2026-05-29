package com.aigm.memory.controller;

import com.aigm.common.feign.dto.RecallRequest;
import com.aigm.common.feign.dto.RecallResult;
import com.aigm.common.feign.dto.StoreRequest;
import com.aigm.common.feign.dto.StoreResult;
import com.aigm.common.result.R;
import com.aigm.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 长程记忆 store/recall（基线 §5.5）。INTERNAL：网关不对外路由，仅供 Feign 内网直连。
 */
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /** 批量向量化并入库。 */
    @PostMapping("/store")
    public R<StoreResult> store(@RequestBody StoreRequest req) {
        return R.ok(memoryService.store(req));
    }

    /** 按 query 语义召回 topK（importance 加权重排）。 */
    @PostMapping("/recall")
    public R<RecallResult> recall(@RequestBody RecallRequest req) {
        return R.ok(memoryService.recall(req));
    }
}
