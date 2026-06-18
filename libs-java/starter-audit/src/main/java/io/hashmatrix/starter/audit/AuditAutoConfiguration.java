package io.hashmatrix.starter.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 审计自动装配：注册默认 {@link AuditRecorder}（slf4j 实现）。
 *
 * <p>可经 {@code hashmatrix.audit.enabled=false} 关闭；带 {@link ConditionalOnMissingBean}，
 * 子仓/security 可提供自定义 {@code AuditRecorder} Bean（如投递 Kafka 审计 topic）覆盖默认实现。
 */
@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(
        prefix = "hashmatrix.audit",
        name = "enabled",
        matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditRecorder hashmatrixAuditRecorder(AuditProperties properties) {
        return new Slf4jAuditRecorder(properties.getLoggerName());
    }
}
