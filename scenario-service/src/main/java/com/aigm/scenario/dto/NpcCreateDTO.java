package com.aigm.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NpcCreateDTO {
    @NotBlank(message = "npcKey 不能为空")
    private String npcKey;
    @NotBlank(message = "name 不能为空")
    private String name;
    @NotBlank(message = "persona 不能为空")
    private String persona;
    private String background;
    private String secret;
    private String avatar;
}
