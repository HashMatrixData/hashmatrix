package io.hashmatrix.starter.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可观测 starter 配置项，前缀 {@code hashmatrix.observability}。
 */
@ConfigurationProperties("hashmatrix.observability")
public class ObservabilityProperties {

    /** 是否启用可观测自动装配（默认启用）。 */
    private boolean enabled = true;

    /**
     * 服务名，作为公共标签 {@code service} 打到所有指标上；
     * 留空则不追加（通常由部署期 {@code spring.application.name} / OTel agent 提供）。
     */
    private String serviceName;

    /** 追加到所有指标的公共标签（如 {@code env=prod}）。 */
    private Map<String, String> commonTags = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getCommonTags() {
        return commonTags;
    }

    public void setCommonTags(Map<String, String> commonTags) {
        this.commonTags = commonTags;
    }
}
