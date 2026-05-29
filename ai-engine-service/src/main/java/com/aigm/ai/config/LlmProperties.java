package com.aigm.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * LLM 配置（前缀 aigm.llm，从 Nacos 配置中心读，支持热刷新，基线 §5.3.1）。
 * api-key 为空时走确定性 STUB，不调用网络。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "aigm.llm")
public class LlmProperties {
    private String provider = "deepseek";
    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private double temperature = 0.8;
    private int maxTokens = 1200;
    private int timeoutMs = 60000;
    private int connectTimeoutMs = 3000;
    /** 对齐基线配置键 aigm.llm.response-format-json / aigm.llm.json-mode，二者择一即可。 */
    private boolean responseFormatJson = true;
    private boolean jsonMode = true;
    private int maxRetry = 1;
    private int networkRetry = 1;

    /** api-key 非空才走真实 LLM；否则 STUB。 */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 是否下发 response_format=json_object（两个配置键任一为真即生效）。 */
    public boolean isJsonModeEnabled() {
        return responseFormatJson || jsonMode;
    }
}
