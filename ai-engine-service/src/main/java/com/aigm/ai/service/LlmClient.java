package com.aigm.ai.service;

import com.aigm.ai.config.LlmProperties;
import com.aigm.ai.llm.ChatMessage;
import com.aigm.ai.llm.OpenAiCompatClient;
import com.aigm.ai.prompt.PromptTemplates;
import com.aigm.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 调用门面（基线 §5.7）：网络/超时重试 + JSON 修复重试（基线 §6.5 第 1 条）。
 * 真正的 schema/白名单校验在 OutputValidator。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final OpenAiCompatClient client;
    private final LlmProperties props;
    private final ObjectMapper objectMapper;

    /** 返回值：清洗后的文本 + 是否可解析 + 实际修复重试次数。 */
    public Result chatForJson(List<ChatMessage> baseMessages) {
        String raw = callWithNetworkRetry(baseMessages);

        int retry = 0;
        String cleaned = stripFences(raw);
        while (!isParseable(cleaned) && retry < props.getMaxRetry()) {
            retry++;
            log.warn("[LLM] output not parseable, repair retry {}/{}", retry, props.getMaxRetry());
            List<ChatMessage> repairMsgs = new ArrayList<>(baseMessages);
            repairMsgs.add(new ChatMessage("assistant", raw));
            repairMsgs.add(new ChatMessage("user", String.format(PromptTemplates.REPAIR_HINT, raw)));
            raw = callWithNetworkRetry(repairMsgs);
            cleaned = stripFences(raw);
        }
        return new Result(cleaned, isParseable(cleaned), retry);
    }

    private String callWithNetworkRetry(List<ChatMessage> messages) {
        int attempt = 0;
        BizException last = null;
        while (attempt <= props.getNetworkRetry()) {
            try {
                return client.chat(messages);
            } catch (BizException e) {
                last = e;
                attempt++;
                log.warn("[LLM] network attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        throw last; // 1500，上层 catch 后兜底降级
    }

    /** 去掉模型可能多包的 ```json ... ``` 围栏。 */
    private String stripFences(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    private boolean isParseable(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            JsonNode node = objectMapper.readTree(s);
            return node.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    public record Result(String json, boolean parseable, int retryCount) {}
}
