package io.hashmatrix.starter.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * 日志关联 starter 配置项，前缀 {@code hashmatrix.logging}。
 */
@ConfigurationProperties("hashmatrix.logging")
public class LoggingProperties {

    /** 是否启用请求级 MDC 注入（默认启用）。 */
    private boolean enabled = true;

    /** 请求标识请求头：存在则沿用（跨服务透传），否则生成。 */
    private String requestIdHeader = "X-Request-Id";

    /** 租户写入 MDC 的键名。 */
    private String tenantMdcKey = "tenantId";

    /** 请求标识写入 MDC 的键名。 */
    private String requestIdMdcKey = "requestId";

    /** 过滤器顺序：略后于租户过滤器（{@code HIGHEST_PRECEDENCE+50}），确保租户已绑定。 */
    private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRequestIdHeader() {
        return requestIdHeader;
    }

    public void setRequestIdHeader(String requestIdHeader) {
        this.requestIdHeader = requestIdHeader;
    }

    public String getTenantMdcKey() {
        return tenantMdcKey;
    }

    public void setTenantMdcKey(String tenantMdcKey) {
        this.tenantMdcKey = tenantMdcKey;
    }

    public String getRequestIdMdcKey() {
        return requestIdMdcKey;
    }

    public void setRequestIdMdcKey(String requestIdMdcKey) {
        this.requestIdMdcKey = requestIdMdcKey;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public void setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
    }
}
