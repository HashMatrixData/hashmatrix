package io.hashmatrix.starter.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Web 基座自动装配：在 Servlet Web 应用下注册全局异常处理。
 *
 * <p>可经 {@code hashmatrix.web.exception-handler.enabled=false} 关闭；带 {@link ConditionalOnMissingBean}，
 * 子仓可覆盖自定义实现。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = "hashmatrix.web.exception-handler",
        name = "enabled",
        matchIfMissing = true)
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler hashmatrixGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
