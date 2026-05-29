package com.aigm.scenario.dto;

import lombok.Data;

import java.util.List;

@Data
public class NodeUpdateDTO {
    private String title;
    private String narrativeBrief;
    private Integer isEnding;
    private String endingType;
    private List<Long> npcIds;
}
