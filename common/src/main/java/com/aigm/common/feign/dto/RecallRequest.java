package com.aigm.common.feign.dto;

import lombok.Data;

/** memory/recall 入参（基线 §5.5）。topK 缺省 5。 */
@Data
public class RecallRequest {
    private Long sessionId;
    private String query;
    private Integer topK;
}
