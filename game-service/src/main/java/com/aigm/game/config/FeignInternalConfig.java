package com.aigm.game.config;

import com.aigm.common.constant.CommonConst;
import com.aigm.common.web.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Feign 内网调用统一带 X-Internal-Call 标识，并透传 X-User-Id（供 memory 按用户/会话隔离，基线 §3.4）。 */
@Configuration
public class FeignInternalConfig {

    @Bean
    public RequestInterceptor internalCallInterceptor() {
        return template -> {
            template.header(CommonConst.HEADER_INTERNAL, "true");
            Long uid = UserContext.getUserId();
            if (uid != null) {
                template.header(CommonConst.HEADER_USER_ID, String.valueOf(uid));
            }
        };
    }
}
