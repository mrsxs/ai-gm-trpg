# NPC 秘密按 evidence 阈值统一解锁，废弃 secret_unlock_flag

**背景**：§8.1 `resolveKnownFacts` 调用 `npc.getSecretUnlockFlag()`、§6.3 称"按 `state.flags` 判断 secret 解锁"，但锁定的 `t_npc` DDL（`id/scenario_id/npc_key/name/persona/background/secret/avatar`）**没有任何解锁字段**——代码与基线 DDL 冲突。需要一个能跑、又不破 DDL 的解锁机制（医生=真凶的 `secret` 不能过早泄露，否则悬疑崩盘）。

**决定**：`secret` 解锁由 game-service 一条规则完成——当 `attr.evidence >= 阈值`（默认 **1**，可配，故意 < 胜利门槛 2，让秘密在对峙前就有机会浮现）时，把该 NPC 的 `secret` 并入 `knownFacts` 下发给 ai-engine；否则只下发 `background`。**废弃** §8.1 的 `secretUnlockFlag` 路径（无 DDL 支撑，实现不了）。

**理由**：(1) 忠于锁定 DDL，不新增列（不破"禁止偏离命名"）；(2) 复用既有 `evidence` 属性，语义自洽——"证据越多，NPC 越兜不住话"；(3) 正是 §8.1 自己给出的 fallback。`evidence` 来自 item（日记 / 凶器）而非 NPC secret，故不存在"要 secret 才有 evidence"的解锁死锁。

**后果**：所有 NPC 同一阈值一起解锁，非逐 NPC 精控；种子只有医生 secret 是关键，够用。要逐 NPC 解锁需改 `t_npc` schema（超出"禁止偏离命名"范围，不做）。
