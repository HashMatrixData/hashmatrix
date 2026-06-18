package io.hashmatrix.starter.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * 多租户上下文 starter 自动装配：仅在 Servlet Web 应用下注册租户过滤器。
 *
 * <p>可经 {@code hashmatrix.tenant.enabled=false} 关闭；过滤器/注册均带 {@link ConditionalOnMissingBean}，
 * 子仓可覆盖自定义实现。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "hashmatrix.tenant", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TenantProperties.class)
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantContextFilter tenantContextFilter(TenantProperties properties) {
        return new TenantContextFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "tenantContextFilterRegistration")
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
            TenantContextFilter filter, TenantProperties properties) {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("tenantContextFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(properties.getFilterOrder());
        return registration;
    }
}
