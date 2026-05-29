package com.aigm.scenario.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_scene_node")
public class SceneNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenarioId;
    private String nodeKey;
    private String title;
    private String narrativeBrief;
    private Integer isEnding;
    private String endingType;
    private Integer sortNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
