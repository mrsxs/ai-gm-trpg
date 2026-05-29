package com.aigm.common.feign.dto;

import lombok.Data;

/** 剧本上下文（GenerateRequest 子对象，基线 §6.3）。 */
@Data
public class ScenarioContext {
    private String title;
    private String genre;
}
