package com.aigm.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/** Prompt 相关配置（前缀 aigm.prompt，基线 §5.3.1）。 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "aigm.prompt")
public class PromptProperties {
    private int narrativeMin = 100;
    private int narrativeMax = 200;
    private int maxRecalled = 5;
}
