package com.aigm.ai.service;

import com.aigm.ai.config.PromptProperties;
import com.aigm.ai.llm.ChatMessage;
import com.aigm.ai.prompt.PromptTemplates;
import com.aigm.common.feign.dto.GameStateDTO;
import com.aigm.common.feign.dto.GenerateRequest;
import com.aigm.common.feign.dto.NodeDTO;
import com.aigm.common.feign.dto.NpcDTO;
import com.aigm.common.feign.dto.RecalledMemory;
import com.aigm.common.feign.dto.ScenarioContext;
import com.aigm.common.feign.dto.TransitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 GenerateRequest 组装成 [system, user] 两条 message（基线 §5.6）。
 * user 分块：剧本上下文→当前节点→出场 NPC→当前状态→召回记忆→合法转移白名单→玩家输入→输出指令。
 */
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptProperties promptProps;

    public List<ChatMessage> build(GenerateRequest req) {
        String system = String.format(PromptTemplates.SYSTEM_GM,
                promptProps.getNarrativeMin(), promptProps.getNarrativeMax());
        String user = buildUser(req);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", system));
        messages.add(new ChatMessage("user", user));
        return messages;
    }

    private String buildUser(GenerateRequest req) {
        StringBuilder sb = new StringBuilder(2048);

        // 1) 剧本上下文
        ScenarioContext ctx = req.getScenarioContext();
        sb.append("【剧本】").append(ctx != null ? ctx.getTitle() : "未知")
          .append("  题材:").append(ctx != null ? ctx.getGenre() : "未知").append('\n');

        // 2) 当前节点叙事约束
        NodeDTO node = req.getCurrentNode();
        sb.append("\n【当前节点】id=").append(node.getId())
          .append(" key=").append(node.getNodeKey())
          .append(" 标题:").append(node.getTitle()).append('\n');
        sb.append("叙事提示要点(请据此推进，不要超纲):\n")
          .append(nz(node.getNarrativeBrief())).append('\n');
        if (Boolean.TRUE.equals(node.getIsEnding())) {
            sb.append("注意:这是结局节点(").append(node.getEndingType())
              .append(")，请收束剧情。\n");
        }

        // 3) 出场 NPC personas（人格一致核心）
        sb.append("\n【出场 NPC（只能扮演以下 NPC，npcId 必须取自这里）】\n");
        if (CollectionUtils.isEmpty(req.getNpcs())) {
            sb.append("(本节点无出场 NPC)\n");
        } else {
            for (NpcDTO n : req.getNpcs()) {
                sb.append(String.format(PromptTemplates.NPC_PERSONA_BLOCK,
                        n.getNpcId(), nz(n.getName()), nz(n.getNpcKey()),
                        nz(n.getPersona()), nz(n.getKnownFacts())));
            }
        }

        // 4) 当前游戏状态
        GameStateDTO gs = req.getGameState();
        sb.append("\n【当前状态】\n");
        sb.append("旗标 flags: ").append(gs.getFlags()).append('\n');
        sb.append("物品 inventory: ").append(gs.getInventory()).append('\n');
        sb.append("属性 attributes: ").append(gs.getAttributes()).append('\n');
        sb.append("近况摘要: ").append(nz(gs.getRecentSummary())).append('\n');

        // 5) 召回记忆（长程记忆/RAG 注入）
        sb.append("\n【相关长程记忆（保持与之一致，勿矛盾）】\n");
        List<RecalledMemory> mems = req.getRecalledMemories();
        if (CollectionUtils.isEmpty(mems)) {
            sb.append("(暂无)\n");
        } else {
            int limit = Math.min(mems.size(), promptProps.getMaxRecalled());
            for (int i = 0; i < limit; i++) {
                RecalledMemory m = mems.get(i);
                sb.append(String.format(PromptTemplates.MEMORY_BLOCK,
                        nz(m.getMemType()), m.getImportance() == null ? 3 : m.getImportance(),
                        nz(m.getContent())));
            }
        }

        // 6) 合法转移白名单（防跑偏核心）
        sb.append("\n【合法转移白名单（proposedTransition.toNodeId 只能取以下整数之一，或 null 表示停留）】\n");
        if (CollectionUtils.isEmpty(node.getTransitions())) {
            sb.append("(当前节点无出边，toNodeId 必须为 null)\n");
        } else {
            for (TransitionDTO t : node.getTransitions()) {
                sb.append(String.format(PromptTemplates.TRANSITION_BLOCK,
                        t.getToNodeId(), nz(t.getCondition()),
                        t.getPriority() == null ? 0 : t.getPriority(), nz(t.getDescription())));
            }
        }

        // 7) 玩家输入
        sb.append("\n【玩家本回合输入】\n");
        if (Boolean.TRUE.equals(req.getIsFirstTurn())) {
            sb.append("(这是开局首回合，请生成开场叙事，引导玩家进入剧情；通常 proposedTransition.toNodeId=null)\n");
        } else {
            sb.append(nz(req.getPlayerInput())).append('\n');
        }

        // 8) 输出指令
        sb.append("\n【请严格按 system 中的 JSON Schema 输出一个合法 JSON 对象，不要任何额外文字。】\n");
        return sb.toString();
    }

    private String nz(String s) { return s == null ? "" : s; }
}
