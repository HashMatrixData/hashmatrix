package io.hashmatrix.starter.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * 可观测自动装配：为所有指标注入平台公共标签（{@code service} + 自定义 commonTags）。
 *
 * <p>actuator 探针与 Prometheus 出口由依赖自带（{@code spring-boot-starter-actuator} +
 * {@code micrometer-registry-prometheus}）；本配置只补统一标签。可经
 * {@code hashmatrix.observability.enabled=false} 关闭。链路（OTel）导出走部署期 Java agent，不绑代码。
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(
        prefix = "hashmatrix.observability",
        name = "enabled",
        matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> hashmatrixCommonTagsCustomizer(
            ObservabilityProperties properties) {
        return registry -> {
            if (StringUtils.hasText(properties.getServiceName())) {
                registry.config().commonTags("service", properties.getServiceName());
            }
            properties
                    .getCommonTags()
                    .forEach((key, value) -> registry.config().commonTags(key, value));
        };
    }
}
