package com.aigm.common.config;

import com.aigm.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * common 自动配置（基线 §9）：仅在引入了 web(MVC) 的服务里装配 GlobalExceptionHandler；
 * gateway(WebFlux) 因无 RestControllerAdvice 类而不装配，避免 WebMVC 类加载报错。
 */
@AutoConfiguration
@ConditionalOnClass(RestControllerAdvice.class)
public class CommonAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
