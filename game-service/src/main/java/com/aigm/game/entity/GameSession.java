package com.aigm.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_game_session")
public class GameSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long scenarioId;
    private String title;
    private Integer status;       // 1进行中 2通关 3失败 4弃局
    private Integer turnCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
