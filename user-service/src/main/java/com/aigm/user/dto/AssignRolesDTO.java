package com.aigm.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRolesDTO {
    @NotEmpty(message = "角色不能为空")
    private List<String> roles;
}
