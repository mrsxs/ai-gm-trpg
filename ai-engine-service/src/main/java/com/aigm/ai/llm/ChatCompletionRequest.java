package com.aigm.ai.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** OpenAI 兼容请求体（基线 §5.3.3）。response_format 为 null 时不下发。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    private Integer max_tokens;
    /** OpenAI 兼容 JSON 模式：{"type":"json_object"}；不需要时置 null。 */
    private Map<String, String> response_format;
}
