package io.hashmatrix.starter.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SecurityAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class));

    @Test
    void registersPreAuthFilterByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(GatewayPreAuthFilter.class));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("hashmatrix.security.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GatewayPreAuthFilter.class));
    }

    @Test
    void bindsHeaderProperties() {
        runner.withPropertyValues(
                        "hashmatrix.security.user-header=X-Auth-User",
                        "hashmatrix.security.roles-header=X-Auth-Roles")
                .run(
                        ctx -> {
                            SecurityProperties props = ctx.getBean(SecurityProperties.class);
                            assertThat(props.getUserHeader()).isEqualTo("X-Auth-User");
                            assertThat(props.getRolesHeader()).isEqualTo("X-Auth-Roles");
                        });
    }
}
