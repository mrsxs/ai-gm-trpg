package com.aigm.common.feign.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 状态变更（基线 §6.4）。五个字段均必填，无变化用空数组/空对象。 */
@Data
public class StateChanges {
    private List<String> setFlags;
    private List<String> clearFlags;
    private List<String> addItems;
    private List<String> removeItems;
    private Map<String, Integer> attrDelta;
}
