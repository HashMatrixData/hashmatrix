package io.hashmatrix.starter.audit;

/**
 * 审计记录器：接收 {@link AuditEvent} 并落地（日志 / 事件总线 / 审计库等）。
 *
 * <p>默认实现 {@link Slf4jAuditRecorder} 走结构化日志；子仓/security 可提供自定义 Bean 覆盖
 * （{@code @ConditionalOnMissingBean}），如投递到 Kafka 审计 topic。
 */
@FunctionalInterface
public interface AuditRecorder {

    /** 记录一条审计事件。实现须保证不抛出影响主流程的异常。 */
    void record(AuditEvent event);
}
