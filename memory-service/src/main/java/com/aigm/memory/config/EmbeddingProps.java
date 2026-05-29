package com.aigm.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 绑定 aigm.embedding.* 嵌入模型配置（OpenAI 兼容 /embeddings）。 */
@Data
@Component
@ConfigurationProperties(prefix = "aigm.embedding")
public class EmbeddingProps {
    /** 嵌入服务 base-url（OpenAI 兼容，拼 /embeddings）。 */
    private String baseUrl;
    /** 嵌入模型名。 */
    private String model;
    /** API Key；为空时走确定性伪向量兜底（本环境默认空）。 */
    private String apiKey;
    /** 向量维度，锁定 1024。 */
    private int dimension = 1024;
}
