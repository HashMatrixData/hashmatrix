package io.hashmatrix.starter.logging;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * 日志关联自动装配：在 Servlet Web 应用下注册 {@link MdcContextFilter}（带顺序，略后于租户过滤器）。
 *
 * <p>可经 {@code hashmatrix.logging.enabled=false} 关闭。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(
        prefix = "hashmatrix.logging",
        name = "enabled",
        matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    public FilterRegistrationBean<Filter> hashmatrixMdcContextFilter(LoggingProperties properties) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(new MdcContextFilter(properties));
        registration.setOrder(properties.getFilterOrder());
        registration.addUrlPatterns("/*");
        return registration;
    }
}
