package com.aigm.memory.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** t_memory 行实体（降级表：embedding 存 REAL[]）。 */
@Data
public class Memory {
    private Long id;
    private Long sessionId;
    private String content;
    private float[] embedding;
    private String memType;
    private Integer importance;
    private LocalDateTime createdAt;
}
