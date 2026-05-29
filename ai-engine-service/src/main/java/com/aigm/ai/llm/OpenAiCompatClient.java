package com.aigm.ai.llm;

import com.aigm.ai.config.LlmProperties;
import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    /** 返回模型文本内容（choices[0].message.content）。 */
    public String chat(List<ChatMessage> messages) {
        ChatCompletionRequest req = new ChatCompletionRequest();
        req.setModel(props.getModel());
        req.setMessages(messages);
        req.setTemperature(props.getTemperature());
        req.setMax_tokens(props.getMaxTokens());
        if (props.isJsonModeEnabled()) {
            req.setResponse_format(Map.of("type", "json_object"));
        }
        try {
            ChatCompletionResponse resp = llmRestClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (resp == null || resp.getChoices() == null || resp.getChoices().isEmpty()
                    || resp.getChoices().get(0).getMessage() == null) {
                throw new BizException(ResultCode.AI_LLM_FAILED, "LLM 返回为空");
            }
            String content = resp.getChoices().get(0).getMessage().getContent();
            log.info("[LLM] provider={} model={} usage={}", props.getProvider(),
                    props.getModel(), resp.getUsage());
            return content == null ? "" : content.trim();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[LLM] call failed: {}", e.getMessage(), e);
            throw new BizException(ResultCode.AI_LLM_FAILED, "LLM 调用失败:" + e.getMessage());
        }
    }
}
