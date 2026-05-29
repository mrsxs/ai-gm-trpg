package com.aigm.game.dto;

import com.aigm.common.feign.dto.GenerateResponse;
import lombok.Data;

@Data
public class TurnVO {
    private Integer turnNo;
    private String playerInput;
    private GenerateResponse aiOutput;
}
