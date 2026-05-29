package com.aigm.common.feign.dto;

import lombok.Data;

/** 召回记忆条目（基线 §5.5 / §6.3）。score 仅 recall 结果含。 */
@Data
public class RecalledMemory {
    private String content;
    private String memType;
    private Integer importance;
    private Double score;
}
