package com.aigm.common.condition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** condition 受控小语法（基线 §6.2.1）单测：编辑期校验 + 运行期求值。 */
class ConditionEvaluatorTest {

    private final Map<String, Boolean> flags = Map.of("has_key", true, "talked", false);
    private final Map<String, Integer> attrs = Map.of("evidence", 2, "sanity", 80);
    private final List<String> inv = List.of("生锈的钥匙");

    @Test
    void alwaysAndEmptyAreTrue() {
        assertTrue(ConditionEvaluator.evaluate("always", flags, attrs, inv));
        assertTrue(ConditionEvaluator.evaluate("", flags, attrs, inv));
        assertTrue(ConditionEvaluator.evaluate(null, flags, attrs, inv));
    }

    @Test
    void flagEquality() {
        assertTrue(ConditionEvaluator.evaluate("flag.has_key==true", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("flag.has_key==false", flags, attrs, inv));
        assertTrue(ConditionEvaluator.evaluate("flag.talked==false", flags, attrs, inv));
        // 缺省 flag 视为 false
        assertTrue(ConditionEvaluator.evaluate("flag.missing==false", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("flag.missing==true", flags, attrs, inv));
    }

    @Test
    void attrComparison() {
        assertTrue(ConditionEvaluator.evaluate("attr.evidence>=2", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("attr.evidence>2", flags, attrs, inv));
        assertTrue(ConditionEvaluator.evaluate("attr.evidence<3", flags, attrs, inv));
        assertTrue(ConditionEvaluator.evaluate("attr.evidence==2", flags, attrs, inv));
        // 缺省属性视为 0
        assertTrue(ConditionEvaluator.evaluate("attr.unknown<=0", flags, attrs, inv));
    }

    @Test
    void itemContains() {
        assertTrue(ConditionEvaluator.evaluate("item.生锈的钥匙", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("item.不存在的物品", flags, attrs, inv));
    }

    @Test
    void andChaining() {
        assertTrue(ConditionEvaluator.evaluate("flag.has_key==true && attr.evidence>=1", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("flag.has_key==true && attr.evidence>=3", flags, attrs, inv));
    }

    @Test
    void unknownAndNotEqualReturnFalse() {
        // 不支持 !=，看不懂 → false
        assertFalse(ConditionEvaluator.evaluate("attr.evidence!=1", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("garbage expr", flags, attrs, inv));
        assertFalse(ConditionEvaluator.evaluate("flag.has_key==true || attr.evidence>=1", flags, attrs, inv));
    }

    @Test
    void syntaxValidation() {
        assertTrue(ConditionEvaluator.isValidSyntax("always"));
        assertTrue(ConditionEvaluator.isValidSyntax(""));
        assertTrue(ConditionEvaluator.isValidSyntax("flag.has_key==true && attr.evidence>=2"));
        assertTrue(ConditionEvaluator.isValidSyntax("item.钥匙"));
        assertFalse(ConditionEvaluator.isValidSyntax("attr.evidence!=2")); // 无 !=
        assertFalse(ConditionEvaluator.isValidSyntax("flag.x")); // flag 必须带 ==bool
        assertFalse(ConditionEvaluator.isValidSyntax("a || b")); // 无 ||
    }
}
