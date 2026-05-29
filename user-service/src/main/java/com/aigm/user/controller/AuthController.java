package com.aigm.user.controller;

import com.aigm.common.result.R;
import com.aigm.user.dto.LoginDTO;
import com.aigm.user.dto.LoginVO;
import com.aigm.user.dto.RegisterDTO;
import com.aigm.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 注册/登录（PUBLIC，网关白名单，基线 §5.1）。 */
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        Long userId = userService.register(dto);
        return R.ok(Map.of("userId", userId));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }
}
