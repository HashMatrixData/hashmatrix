package io.hashmatrix.starter.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计 starter 配置项，前缀 {@code hashmatrix.audit}。
 */
@ConfigurationProperties("hashmatrix.audit")
public class AuditProperties {

    /** 是否启用审计自动装配（默认启用）。 */
    private boolean enabled = true;

    /** 审计 logger 名；可在日志配置里单独路由审计通道。 */
    private String loggerName = "hashmatrix.audit";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }
}
