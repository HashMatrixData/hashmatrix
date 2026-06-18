package io.hashmatrix.starter.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class TenantAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class));

    private final ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class));

    @Test
    void registersFilterInServletWebApp() {
        webRunner.run(context -> {
            assertThat(context).hasSingleBean(TenantContextFilter.class);
            assertThat(context).hasSingleBean(FilterRegistrationBean.class);
            assertThat(context).hasSingleBean(TenantProperties.class);
        });
    }

    @Test
    void bindsCustomProperties() {
        webRunner
                .withPropertyValues(
                        "hashmatrix.tenant.header=X-Org",
                        "hashmatrix.tenant.required=true",
                        "hashmatrix.tenant.filter-order=42")
                .run(context -> {
                    TenantProperties properties = context.getBean(TenantProperties.class);
                    assertThat(properties.getHeader()).isEqualTo("X-Org");
                    assertThat(properties.isRequired()).isTrue();
                    assertThat(properties.getFilterOrder()).isEqualTo(42);
                });
    }

    @Test
    void backsOffWhenDisabled() {
        webRunner
                .withPropertyValues("hashmatrix.tenant.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TenantContextFilter.class));
    }

    @Test
    void backsOffOutsideServletWebApp() {
        nonWebRunner.run(context -> assertThat(context).doesNotHaveBean(TenantContextFilter.class));
    }
}
