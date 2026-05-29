package com.aigm.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusDTO {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
