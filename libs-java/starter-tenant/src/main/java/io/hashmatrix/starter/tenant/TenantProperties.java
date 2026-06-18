package io.hashmatrix.starter.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * 多租户上下文 starter 配置项，前缀 {@code hashmatrix.tenant}。
 */
@ConfigurationProperties("hashmatrix.tenant")
public class TenantProperties {

    /** 是否启用租户上下文过滤器（默认启用）。 */
    private boolean enabled = true;

    /** 承载租户标识的请求头（由网关注入，见架构 05 §5）。 */
    private String header = "X-Tenant-Id";

    /** 承载原始 org 标识的请求头（可选）。 */
    private String orgHeader = "X-Tenant-Org";

    /**
     * 是否强制要求租户头：为 {@code true} 时，缺头请求直接以 400 拒绝；
     * 为 {@code false}（默认）时放行，由下游自行决定（如公共/匿名端点）。
     */
    private boolean required = false;

    /** 过滤器顺序：缺省靠前，确保业务/数据访问前已建立上下文。 */
    private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getOrgHeader() {
        return orgHeader;
    }

    public void setOrgHeader(String orgHeader) {
        this.orgHeader = orgHeader;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public void setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
    }
}
