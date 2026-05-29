package com.aigm.scenario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublishDTO {
    /** 目标状态：1 发布，2 下架（0 视作下架）。 */
    @NotNull(message = "status 不能为空")
    private Integer status;
}
