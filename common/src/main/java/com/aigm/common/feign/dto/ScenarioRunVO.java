package com.aigm.common.feign.dto;

import lombok.Data;

/** 运行时剧本精简定义（scenario → game，开局所需，基线 §5.2 run 接口）。 */
@Data
public class ScenarioRunVO {
    private Long id;
    private String title;
    private String genre;
    private Long startNodeId;
}
