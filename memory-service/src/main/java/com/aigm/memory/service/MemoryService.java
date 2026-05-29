package com.aigm.memory.service;

import com.aigm.common.feign.dto.RecallRequest;
import com.aigm.common.feign.dto.RecallResult;
import com.aigm.common.feign.dto.StoreRequest;
import com.aigm.common.feign.dto.StoreResult;

/** 长程记忆 store/recall。 */
public interface MemoryService {

    StoreResult store(StoreRequest request);

    RecallResult recall(RecallRequest request);
}
