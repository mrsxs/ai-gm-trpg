package com.aigm.scenario.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_npc")
public class Npc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenarioId;
    private String npcKey;
    private String name;
    private String persona;
    private String background;
    private String secret;
    private String avatar;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
