package com.aigm.common.result;

import lombok.Getter;

/**
 * 全局唯一错误码枚举（基线 §3.2，枚举名即唯一标准，禁止造别名）。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),

    // ===== 鉴权 AUTH 1000–1099 =====
    AUTH_NOT_LOGIN(1001, "未登录或登录已失效"),
    AUTH_BAD_CREDENTIALS(1002, "用户名或密码错误"),
    AUTH_TOKEN_EXPIRED(1003, "登录已过期，请重新登录"),
    AUTH_TOKEN_INVALID(1004, "Token 非法"),
    AUTH_NO_PERMISSION(1005, "无权限访问"),
    AUTH_ACCOUNT_DISABLED(1006, "账号已被禁用"),

    // ===== 参数 PARAM 1100–1199 =====
    PARAM_INVALID(1100, "参数校验失败"),
    PARAM_MISSING(1101, "缺少必填参数"),
    PARAM_FORMAT_ERROR(1102, "参数格式错误"),
    PAGE_PARAM_INVALID(1103, "分页参数非法"),

    // ===== 业务 BIZ 1200–1499 =====
    RESOURCE_NOT_FOUND(1200, "资源不存在"),
    RESOURCE_EXISTS(1201, "资源已存在或重复"),
    STATE_INVALID(1202, "状态非法"),
    SCENARIO_NO_START_NODE(1210, "剧本无起始节点"),
    NODE_NO_TRANSITION(1211, "节点无可用分支"),
    GAME_SESSION_NOT_FOUND(1220, "对局不存在"),
    GAME_SESSION_FORBIDDEN(1221, "无权访问该对局"),

    // ===== AI 1500–1599 =====
    AI_LLM_FAILED(1500, "AI 调用失败或超时"),
    AI_LLM_BAD_JSON(1501, "AI 返回非合法 JSON"),
    AI_OUTPUT_SCHEMA_INVALID(1502, "AI 输出 schema 校验失败"),
    AI_TRANSITION_REJECTED(1503, "AI 提议的剧情跳转越界被拒"),
    AI_EMBEDDING_FAILED(1504, "嵌入向量生成失败"),

    // ===== 系统 SYS 1900–1999 =====
    SYSTEM_ERROR(1900, "系统内部错误"),
    SERVICE_UNAVAILABLE(1901, "下游服务不可用"),
    DB_ERROR(1902, "数据库错误"),
    RATE_LIMITED(1903, "请求过于频繁，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
