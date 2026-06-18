package io.hashmatrix.starter.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.hashmatrix.starter.tenant.TenantContext;
import io.hashmatrix.starter.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    void registersDefaultRecorderByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(AuditRecorder.class));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("hashmatrix.audit.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditRecorder.class));
    }

    @Test
    void backsOffWhenCustomRecorderPresent() {
        runner.withBean("customRecorder", AuditRecorder.class, () -> event -> {})
                .run(ctx -> assertThat(ctx).hasSingleBean(AuditRecorder.class)
                        .getBean(AuditRecorder.class)
                        .extracting(Object::getClass)
                        .isNotEqualTo(Slf4jAuditRecorder.class));
    }

    @Test
    void eventStampsCurrentTenant() {
        TenantContextHolder.runWith(
                TenantContext.of("tenant-demo"),
                () -> {
                    AuditEvent event =
                            AuditEvent.of(
                                    "acme-user",
                                    "PUBLISH",
                                    "model:42",
                                    AuditEvent.Outcome.SUCCESS,
                                    null);
                    assertThat(event.tenantId()).isEqualTo("tenant-demo");
                    assertThat(event.timestamp()).isNotNull();
                });
    }
}
