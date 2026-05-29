package com.aigm.game.util;

import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** t_game_state 的 JSON 字段（flags/inventory/attributes）与 Java 容器互转。 */
public final class StateCodec {

    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<Map<String, Boolean>> FLAGS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Integer>> ATTRS = new TypeReference<>() {};
    private static final TypeReference<List<String>> ITEMS = new TypeReference<>() {};

    private StateCodec() {}

    public static Map<String, Boolean> readFlags(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return M.readValue(json, FLAGS);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static Map<String, Integer> readAttributes(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return M.readValue(json, ATTRS);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static List<String> readInventory(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return M.readValue(json, ITEMS);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String toJson(Object o) {
        try {
            return M.writeValueAsString(o);
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "状态序列化失败");
        }
    }
}
