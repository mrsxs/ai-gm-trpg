package com.aigm.game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitTurnDTO {
    @NotBlank(message = "playerInput 不能为空")
    private String playerInput;
}
