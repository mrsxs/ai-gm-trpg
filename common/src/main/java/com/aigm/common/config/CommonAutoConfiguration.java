package com.aigm.common.config;

import com.aigm.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * common 自动配置（基线 §9）：仅在 SERVLET(WebMVC) 服务里装配 GlobalExceptionHandler；
 * gateway(WebFlux/REACTIVE) 不装配，避免误接管网关异常（RestControllerAdvice 注解在 spring-web，
 * WebFlux 也存在，故用 ConditionalOnWebApplication(SERVLET) 精确区分）。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
