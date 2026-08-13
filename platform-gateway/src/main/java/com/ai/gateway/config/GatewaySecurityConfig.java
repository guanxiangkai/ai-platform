package com.ai.gateway.config;

import io.github.guanxiangkai.web.plus.core.properties.TrustedForwardProperties;
import io.github.guanxiangkai.web.plus.security.handler.CustomAuthenticationEntryPoint;
import com.ai.gateway.filter.GatewayJwtAuthFilter;
import com.ai.gateway.util.GatewayPathMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/**
 * 网关独立的 Security 配置
 * <p>
 * 网关自行管理 RSA 公钥验证 JWT + Redis 黑名单检查，
 * 公共 bean（CorsConfigurationSource、PasswordEncoder 等）复用 {@code web-plus-security} 提供的。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(
            ServerHttpSecurity http,
            AiGatewayProperties props,
            TrustedForwardProperties trustedForwardProperties,
            @Qualifier("authReactiveStringRedisTemplate") ReactiveStringRedisTemplate authRedisTemplate,
            CustomAuthenticationEntryPoint authenticationEntryPoint) {

        trustedForwardProperties.validateConfigured("Gateway 身份透传");
        GatewayJwtAuthFilter jwtAuthFilter = new GatewayJwtAuthFilter(props, authRedisTemplate);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // CORS 由 Gateway globalcors 统一处理，Security 层不再重复配置，避免双重 Access-Control-Allow-Origin
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 动态白名单：每次请求实时读取 @RefreshScope 代理的 excludePaths，
                        // 与 GatewayJwtAuthFilter.shouldSkip() 保持一致，
                        // 避免 Nacos 配置刷新后 SecurityWebFilterChain 的静态快照过期。
                        .anyExchange().access((authentication, context) -> {
                            String path = context.getExchange().getRequest().getURI().getPath();
                            boolean excluded = GatewayPathMatcher.matchesAny(props.getExcludePaths(), path);
                            if (excluded) {
                                return Mono.just(new AuthorizationDecision(true));
                            }
                            return authentication
                                    .map(auth2 -> {
                                        log.debug("[SecurityAccess] path={}, isAuthenticated={}, principal={}",
                                                path, auth2.isAuthenticated(), auth2.getPrincipal());
                                        return auth2.isAuthenticated();
                                    })
                                    .defaultIfEmpty(false)
                                    .doOnNext(granted -> {
                                        if (!granted) {
                                            log.warn("[SecurityAccess] 拒绝访问: path={}, 原因=无有效认证", path);
                                        }
                                    })
                                    .map(AuthorizationDecision::new);
                        }))
                .addFilterBefore(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

}
