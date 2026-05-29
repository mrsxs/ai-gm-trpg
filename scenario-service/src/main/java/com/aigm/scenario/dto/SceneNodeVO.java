package com.aigm.scenario.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SceneNodeVO {
    private Long id;
    private Long scenarioId;
    private String nodeKey;
    private String title;
    private String narrativeBrief;
    private Integer isEnding;
    private String endingType;
    private Integer sortNo;
    private List<Long> npcIds;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
