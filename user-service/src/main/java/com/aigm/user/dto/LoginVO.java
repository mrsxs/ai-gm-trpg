package com.aigm.user.dto;

import lombok.Data;

/** 登录返回（基线 §5.1）：{token, userInfo}。 */
@Data
public class LoginVO {
    private String token;
    private UserVO userInfo;
}
