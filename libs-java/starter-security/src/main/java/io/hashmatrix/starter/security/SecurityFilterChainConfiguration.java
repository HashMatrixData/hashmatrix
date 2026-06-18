package io.hashmatrix.starter.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 默认安全过滤链：无状态、放行探针/指标、其余需认证，并前置 {@link GatewayPreAuthFilter}；
 * 同时开启方法级授权（{@link EnableMethodSecurity}）。仅 Servlet Web 应用下生效。
 *
 * <p>子仓可提供自定义 {@code SecurityFilterChain} Bean 覆盖（{@code @ConditionalOnMissingBean}）。
 */
@AutoConfiguration(
        after = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableMethodSecurity
@ConditionalOnProperty(
        prefix = "hashmatrix.security",
        name = "enabled",
        matchIfMissing = true)
public class SecurityFilterChainConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain hashmatrixSecurityFilterChain(
            HttpSecurity http, GatewayPreAuthFilter preAuthFilter, SecurityProperties properties)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        registry ->
                                registry.requestMatchers(
                                                properties.getPermitPaths().toArray(new String[0]))
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(preAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
