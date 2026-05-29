package com.aigm.scenario.dto;

import lombok.Data;

@Data
public class ScenarioUpdateDTO {
    private String title;
    private String intro;
    private String genre;
    private String cover;
    private Long startNodeId;
}
