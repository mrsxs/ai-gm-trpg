package com.aigm.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * LLM 用同步 RestClient（Spring 6 内置），统一连接/读超时（基线 §5.3.3 备选写法）。
 * 用 SimpleClientHttpRequestFactory 以规避 Boot 版本差异。
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient llmRestClient(LlmProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getTimeoutMs());
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getBaseUrl())
                .build();
    }
}
