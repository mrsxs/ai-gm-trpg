package com.aigm.common.feign.dto;

import lombok.Data;

/** memory/store 返回（基线 §5.5）。 */
@Data
public class StoreResult {
    private Integer storedCount;
}
