package com.aigm.common.constant;

/** 网关透传头与内部调用头（基线 §3.3 / §3.4）。 */
public final class CommonConst {
    private CommonConst() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_INTERNAL = "X-Internal-Call";
    public static final String ROLE_SEPARATOR = ",";
}
