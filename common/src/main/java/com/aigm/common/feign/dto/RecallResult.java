package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;

/** memory/recall 返回（基线 §5.5）。 */
@Data
public class RecallResult {
    private List<RecalledMemory> memories;
}
