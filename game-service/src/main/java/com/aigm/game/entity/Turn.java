package com.aigm.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_turn")
public class Turn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer turnNo;
    private Long nodeId;
    private String playerInput;
    private String aiOutput;     // 完整 GenerateResponse JSON
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
