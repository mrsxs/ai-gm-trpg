package com.aigm.ai.llm;

import com.aigm.ai.config.LlmProperties;
import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 chat/completions 客户端，provider 无关（DeepSeek/GLM/Qwen 同协议，基线 §5.3.3）。
 * 网络/HTTP 错误统一抛 BizException(1500)，由上层决定网络重试或兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatClient {

    private final RestClient llmRestClient;
    private final LlmProperties props;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 返回模型文本内容（choices[0].message.content）。 */
    public String chat(List<ChatMessage> messages) {
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setModel(props.getModel());
        req.setMessages(messages);
        req.setTemperature(props.getTemperature());
        req.setMax_tokens(props.getMaxTokens());
        req.setStream(Boolean.FALSE);   // 显式非流式
        if (props.isJsonModeEnabled()) {
            req.setResponse_format(Map.of("type", "json_object"));
        }
        try {
            // 按 byte[] 取回 body 再显式 UTF-8 解码：部分中转对推理模型（如 gpt-5.x）把 content-type
            // 谎报成 text/event-stream 且不带 charset，若按 String 取回，Spring 会默认用 ISO-8859-1 解码
            // 导致中文乱码；这里不依赖 content-type，拿原始字节自行按 UTF-8 解码并解析。
            byte[] rawBytes = llmRestClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(byte[].class);
            String raw = rawBytes == null ? null : new String(rawBytes, StandardCharsets.UTF_8);

            String content = extractContent(raw);
            log.info("[LLM] provider={} model={} contentLen={}", props.getProvider(),
                    props.getModel(), content == null ? 0 : content.length());
            return content == null ? "" : content.trim();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[LLM] call failed: {}", e.getMessage(), e);
            throw new BizException(ResultCode.AI_LLM_FAILED, "LLM 调用失败:" + e.getMessage());
        }
    }

    /**
     * 从原始响应体抽取 choices[0].message.content。兼容三种形态：
     * ① 普通 chat.completion JSON（含 content-type 被谎报为 text/event-stream 但 body 仍是整段 JSON）；
     * ② 真 SSE 流：多行 data: {chunk} 累加 delta.content，data:[DONE] 结束。
     */
    private String extractContent(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(ResultCode.AI_LLM_FAILED, "LLM 返回为空");
        }
        String body = raw.trim();
        try {
            // 形态①：整段 JSON（最常见，包括 content-type 谎报场景）
            if (body.startsWith("{")) {
                JsonNode root = MAPPER.readTree(body);
                JsonNode c = root.path("choices").path(0).path("message").path("content");
                if (!c.isMissingNode() && !c.isNull()) return c.asText("");
            }
            // 形态②：SSE 流，逐行累加 delta.content
            if (body.contains("data:")) {
                StringBuilder sb = new StringBuilder();
                for (String line : body.split("\n")) {
                    line = line.trim();
                    if (!line.startsWith("data:")) continue;
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty() || "[DONE]".equals(payload)) continue;
                    JsonNode chunk = MAPPER.readTree(payload);
                    JsonNode msg = chunk.path("choices").path(0).path("message").path("content");
                    JsonNode delta = chunk.path("choices").path(0).path("delta").path("content");
                    if (!msg.isMissingNode() && !msg.isNull()) sb.append(msg.asText(""));
                    else if (!delta.isMissingNode() && !delta.isNull()) sb.append(delta.asText(""));
                }
                if (sb.length() > 0) return sb.toString();
            }
        } catch (Exception parse) {
            log.error("[LLM] 解析响应失败: {} | body前200={}", parse.getMessage(),
                    body.length() > 200 ? body.substring(0, 200) : body);
        }
        throw new BizException(ResultCode.AI_LLM_FAILED, "LLM 返回无法解析出 content");
    }
}
