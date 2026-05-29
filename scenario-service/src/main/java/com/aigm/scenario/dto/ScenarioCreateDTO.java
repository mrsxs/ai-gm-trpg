package com.aigm.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScenarioCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String intro;
    private String genre;
    private String cover;
}
