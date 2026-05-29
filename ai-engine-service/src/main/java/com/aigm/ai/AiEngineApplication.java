package com.aigm.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** ai-engine-service（端口 8084，INTERNAL）：无状态 状态+输入 → PromptBuilder → LLM → OutputValidator → 结构化输出。 */
@SpringBootApplication
@EnableDiscoveryClient
public class AiEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiEngineApplication.class, args);
    }
}
