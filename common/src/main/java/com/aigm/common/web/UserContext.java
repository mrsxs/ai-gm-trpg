package com.aigm.common.web;

import com.aigm.common.constant.CommonConst;
import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 下游服务读取网关下发的身份头（基线 §3.3）做方法级 RBAC，禁止重复解析 token。
 * 仅在 WebMVC 服务可用（gateway 不使用）。
 */
public final class UserContext {

    private UserContext() {}

    private static HttpServletRequest request() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    public static Long getUserId() {
        HttpServletRequest req = request();
        if (req == null) return null;
        String v = req.getHeader(CommonConst.HEADER_USER_ID);
        if (v == null || v.isBlank()) return null;
        try {
            return Long.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getUsername() {
        HttpServletRequest req = request();
        return req == null ? null : req.getHeader(CommonConst.HEADER_USER_NAME);
    }

    public static List<String> getRoles() {
        HttpServletRequest req = request();
        if (req == null) return Collections.emptyList();
        String v = req.getHeader(CommonConst.HEADER_USER_ROLES);
        if (v == null || v.isBlank()) return Collections.emptyList();
        List<String> roles = new ArrayList<>();
        for (String r : v.split(CommonConst.ROLE_SEPARATOR)) {
            if (!r.isBlank()) roles.add(r.trim());
        }
        return roles;
    }

    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /** 需登录：无 userId 抛 1001。 */
    public static Long requireUserId() {
        Long uid = getUserId();
        if (uid == null) throw new BizException(ResultCode.AUTH_NOT_LOGIN);
        return uid;
    }

    /** 需任一角色：不满足抛 1005。 */
    public static void requireAnyRole(String... roles) {
        List<String> mine = getRoles();
        boolean ok = Arrays.stream(roles).anyMatch(mine::contains);
        if (!ok) throw new BizException(ResultCode.AUTH_NO_PERMISSION);
    }
}
