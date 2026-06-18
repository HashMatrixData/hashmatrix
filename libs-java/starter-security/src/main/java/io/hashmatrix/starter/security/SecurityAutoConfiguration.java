package io.hashmatrix.starter.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 应用侧鉴权自动装配（基础部分）：注册 {@link GatewayPreAuthFilter}。
 *
 * <p>默认安全过滤链与方法级授权见 {@link SecurityFilterChainConfiguration}（仅 Servlet Web 下生效）。
 * 可经 {@code hashmatrix.security.enabled=false} 整体关闭。
 */
@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(
        prefix = "hashmatrix.security",
        name = "enabled",
        matchIfMissing = true)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayPreAuthFilter hashmatrixGatewayPreAuthFilter(SecurityProperties properties) {
        return new GatewayPreAuthFilter(properties);
    }
}
