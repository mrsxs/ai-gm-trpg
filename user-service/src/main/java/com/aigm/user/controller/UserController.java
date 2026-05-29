package com.aigm.user.controller;

import com.aigm.common.result.R;
import com.aigm.common.web.UserContext;
import com.aigm.user.dto.UpdateProfileDTO;
import com.aigm.user.dto.UserVO;
import com.aigm.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 当前登录用户（任意登录角色，身份取自网关 X-User-* 头）。 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.currentUser(UserContext.requireUserId()));
    }

    @PutMapping("/me")
    public R<Boolean> updateMe(@RequestBody UpdateProfileDTO dto) {
        userService.updateProfile(UserContext.requireUserId(), dto);
        return R.ok(true);
    }
}
