package com.aigm.game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartSessionDTO {
    @NotNull(message = "scenarioId 不能为空")
    private Long scenarioId;
}
