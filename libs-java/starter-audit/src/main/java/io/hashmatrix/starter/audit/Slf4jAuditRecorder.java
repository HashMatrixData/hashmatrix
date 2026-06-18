package io.hashmatrix.starter.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认审计记录器：以独立 logger 输出结构化审计行，便于采集到日志管道（Loki/ES）做审计留痕。
 *
 * <p>logger 名可配（{@code hashmatrix.audit.logger-name}，默认 {@code hashmatrix.audit}），
 * 便于在日志配置里把审计单独路由到审计文件/通道。
 */
public class Slf4jAuditRecorder implements AuditRecorder {

    private final Logger log;

    public Slf4jAuditRecorder(String loggerName) {
        this.log = LoggerFactory.getLogger(loggerName);
    }

    @Override
    public void record(AuditEvent event) {
        // 结构化键值，下游可解析；detail 由调用方保证不含凭据/客户可识别信息（红线）。
        log.info(
                "audit ts={} tenant={} actor={} action={} target={} outcome={} detail={}",
                event.timestamp(),
                event.tenantId(),
                event.actor(),
                event.action(),
                event.target(),
                event.outcome(),
                event.detail());
    }
}
