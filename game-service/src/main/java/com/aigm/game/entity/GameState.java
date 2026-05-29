package com.aigm.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_game_state")
public class GameState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long currentNodeId;
    private String flags;        // JSON: {"has_key":true}
    private String inventory;    // JSON: ["生锈的钥匙"]
    private String attributes;   // JSON: {"sanity":80,"evidence":0}
    private String recentSummary;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
