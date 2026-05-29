package com.aigm.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class NodeCreateDTO {
    @NotBlank(message = "nodeKey 不能为空")
    private String nodeKey;
    @NotBlank(message = "title 不能为空")
    private String title;
    @NotBlank(message = "narrativeBrief 不能为空")
    private String narrativeBrief;
    private Integer isEnding;
    private String endingType;
    private List<Long> npcIds;
}
