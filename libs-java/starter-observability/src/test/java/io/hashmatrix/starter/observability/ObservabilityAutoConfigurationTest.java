package io.hashmatrix.starter.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void registersCommonTagsCustomizerByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(MeterRegistryCustomizer.class));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("hashmatrix.observability.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(MeterRegistryCustomizer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesServiceAndCommonTags() {
        runner.withPropertyValues(
                        "hashmatrix.observability.service-name=governance",
                        "hashmatrix.observability.common-tags.env=test")
                .run(
                        ctx -> {
                            MeterRegistryCustomizer<MeterRegistry> customizer =
                                    ctx.getBean(MeterRegistryCustomizer.class);
                            SimpleMeterRegistry registry = new SimpleMeterRegistry();
                            customizer.customize(registry);
                            registry.counter("sample").increment();
                            assertThat(registry.find("sample").counter().getId().getTag("service"))
                                    .isEqualTo("governance");
                            assertThat(registry.find("sample").counter().getId().getTag("env"))
                                    .isEqualTo("test");
                        });
    }
}
