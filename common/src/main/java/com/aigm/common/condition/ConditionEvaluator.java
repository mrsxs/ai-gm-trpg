package com.aigm.common.condition;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * condition 受控小语法（基线 §6.2.1）的唯一实现——编辑期(scenario 发布校验)与运行期(game 权威求值)共用，
 * 保证两端语法集合完全一致。
 *
 * <p>支持原子：
 * <ul>
 *   <li>{@code always}（恒真）；空表达式等价 always</li>
 *   <li>{@code flag.<key>==true|false}（state.flags，缺省 false）</li>
 *   <li>{@code attr.<key><op><整数>}，op ∈ {@code >= <= > < ==}（state.attributes，缺省 0；无 !=）</li>
 *   <li>{@code item.<物品名>}（state.inventory 是否包含）</li>
 *   <li>用 {@code  && } 串联多个原子（全真才真；只支持 &&，不支持 ||）</li>
 * </ul>
 * 求值原则：看不懂的表达式一律返回 false（宁可少放行）。
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {}

    private static final Pattern FLAG = Pattern.compile("^flag\\.[A-Za-z0-9_]+==(true|false)$");
    private static final Pattern ATTR = Pattern.compile("^attr\\.[A-Za-z0-9_]+(>=|<=|==|>|<)-?\\d+$");
    private static final Pattern ITEM = Pattern.compile("^item\\..+$");

    /** 校验整条表达式是否合法（编辑期/发布校验用）。空或 always 合法。 */
    public static boolean isValidSyntax(String expr) {
        if (expr == null || expr.isBlank()) return true;
        String e = expr.trim();
        if ("always".equals(e)) return true;
        for (String atom : e.split(" && ")) {
            if (!isValidAtom(atom.trim())) return false;
        }
        return true;
    }

    private static boolean isValidAtom(String atom) {
        if (atom.isEmpty()) return false;
        return FLAG.matcher(atom).matches()
                || ATTR.matcher(atom).matches()
                || ITEM.matcher(atom).matches();
    }

    /** 运行期求值（基线 §6.5 权威求值在 game-service，应用 stateChanges 后调用）。 */
    public static boolean evaluate(String expr,
                                   Map<String, Boolean> flags,
                                   Map<String, Integer> attributes,
                                   List<String> inventory) {
        if (expr == null || expr.isBlank()) return true;
        String e = expr.trim();
        if ("always".equals(e)) return true;
        for (String atom : e.split(" && ")) {
            if (!evalAtom(atom.trim(), flags, attributes, inventory)) return false;
        }
        return true;
    }

    private static boolean evalAtom(String atom,
                                    Map<String, Boolean> flags,
                                    Map<String, Integer> attributes,
                                    List<String> inventory) {
        try {
            if (FLAG.matcher(atom).matches()) {
                int eq = atom.indexOf("==");
                String key = atom.substring("flag.".length(), eq);
                boolean want = Boolean.parseBoolean(atom.substring(eq + 2));
                boolean cur = flags != null && Boolean.TRUE.equals(flags.get(key));
                return cur == want;
            }
            if (ATTR.matcher(atom).matches()) {
                return evalAttr(atom, attributes);
            }
            if (ITEM.matcher(atom).matches()) {
                String name = atom.substring("item.".length());
                return inventory != null && inventory.contains(name);
            }
        } catch (RuntimeException ignore) {
            return false;
        }
        return false; // 看不懂 → false
    }

    private static boolean evalAttr(String atom, Map<String, Integer> attributes) {
        String op;
        if (atom.contains(">=")) op = ">=";
        else if (atom.contains("<=")) op = "<=";
        else if (atom.contains("==")) op = "==";
        else if (atom.contains(">")) op = ">";
        else if (atom.contains("<")) op = "<";
        else return false;
        int opIdx = atom.indexOf(op);
        String key = atom.substring("attr.".length(), opIdx);
        int rhs = Integer.parseInt(atom.substring(opIdx + op.length()));
        int cur = attributes == null ? 0 : attributes.getOrDefault(key, 0);
        return switch (op) {
            case ">=" -> cur >= rhs;
            case "<=" -> cur <= rhs;
            case ">" -> cur > rhs;
            case "<" -> cur < rhs;
            case "==" -> cur == rhs;
            default -> false;
        };
    }
}
