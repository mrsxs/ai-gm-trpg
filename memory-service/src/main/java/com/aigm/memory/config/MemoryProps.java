package com.aigm.memory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 绑定 aigm.memory.* 记忆治理参数。 */
@Data
@Component
@ConfigurationProperties(prefix = "aigm.memory")
public class MemoryProps {
    /** 是否启用 pgvector（远端未装，默认 false：走 REAL[] 降级表 + 应用层余弦）。 */
    private boolean pgvectorEnabled = false;
    /** recall 默认 topK。 */
    private int recallDefaultTopK = 5;
}
