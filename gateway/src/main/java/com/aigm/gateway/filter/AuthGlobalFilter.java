package com.aigm.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.aigm.common.constant.CommonConst;
import com.aigm.common.result.R;
import com.aigm.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局鉴权（基线 §3.3，鉴权改用 sa-token）：
 * 1) 白名单/预检放行；2) 用 sa-token 校验 token（读共享 Redis 会话）；
 * 3) 剥离客户端伪造的 X-User-* / X-Internal-Call 头，权威下发 userId/username/roles；
 * 4) 校验失败统一返回 401 + R{code=1001}。下游信任网关头，不再解析 token。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
            "/api/user/auth/login",
            "/api/user/auth/register",
            "/doc/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**",
            "/favicon.ico"
    );

    private final ObjectMapper objectMapper;

    public AuthGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // CORS 预检放行
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange.mutate().request(stripIdentityHeaders(request)).build());
        }
        // 白名单放行（仍剥离伪造头）
        if (isWhitelisted(path)) {
            return chain.filter(exchange.mutate().request(stripIdentityHeaders(request)).build());
        }

        SaReactorSyncHolder.setContext(exchange);
        try {
            StpUtil.checkLogin();
            long userId = StpUtil.getLoginIdAsLong();
            SaSession session = StpUtil.getSession();
            String username = session.getString("username");
            Object rolesObj = session.get("roles");
            String roles = rolesObj instanceof List<?> list
                    ? String.join(CommonConst.ROLE_SEPARATOR, list.stream().map(String::valueOf).toList())
                    : "";

            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> {
                        h.remove(CommonConst.HEADER_USER_ID);
                        h.remove(CommonConst.HEADER_USER_NAME);
                        h.remove(CommonConst.HEADER_USER_ROLES);
                        h.remove(CommonConst.HEADER_INTERNAL);
                        h.set(CommonConst.HEADER_USER_ID, String.valueOf(userId));
                        if (username != null) h.set(CommonConst.HEADER_USER_NAME, username);
                        h.set(CommonConst.HEADER_USER_ROLES, roles);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (NotLoginException e) {
            return unauthorized(exchange, e);
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate().headers(h -> {
            h.remove(CommonConst.HEADER_USER_ID);
            h.remove(CommonConst.HEADER_USER_NAME);
            h.remove(CommonConst.HEADER_USER_ROLES);
            h.remove(CommonConst.HEADER_INTERNAL);
        }).build();
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, NotLoginException e) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ResultCode rc = NotLoginException.TOKEN_TIMEOUT.equals(e.getType())
                ? ResultCode.AUTH_TOKEN_EXPIRED
                : ResultCode.AUTH_NOT_LOGIN;
        R<Void> body = R.fail(rc);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            bytes = ("{\"code\":" + rc.getCode() + ",\"message\":\"" + rc.getMessage() + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
