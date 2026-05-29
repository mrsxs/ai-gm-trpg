package com.aigm.user.controller;

import com.aigm.common.constant.RoleConst;
import com.aigm.common.result.R;
import com.aigm.common.util.PageQuery;
import com.aigm.common.util.PageResult;
import com.aigm.common.web.UserContext;
import com.aigm.user.dto.AssignRolesDTO;
import com.aigm.user.dto.UpdateStatusDTO;
import com.aigm.user.dto.UserVO;
import com.aigm.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 管理员用户管理（ADMIN，基线 §5.1）。RBAC 基于网关下发的 X-User-Roles。 */
@RestController
@RequestMapping("/api/user/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public R<PageResult<UserVO>> list(@RequestParam(required = false) Long page,
                                      @RequestParam(required = false) Long size,
                                      @RequestParam(required = false) String username) {
        UserContext.requireAnyRole(RoleConst.ADMIN);
        return R.ok(userService.pageUsers(PageQuery.of(page, size), username));
    }

    @PutMapping("/{id}/status")
    public R<Boolean> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusDTO dto) {
        UserContext.requireAnyRole(RoleConst.ADMIN);
        userService.updateStatus(id, dto.getStatus());
        return R.ok(true);
    }

    @PutMapping("/{id}/roles")
    public R<Boolean> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesDTO dto) {
        UserContext.requireAnyRole(RoleConst.ADMIN);
        userService.assignRoles(id, dto.getRoles());
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        UserContext.requireAnyRole(RoleConst.ADMIN);
        userService.deleteUser(id);
        return R.ok(true);
    }
}
