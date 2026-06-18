package io.hashmatrix.starter.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextHolderTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void setAndGetRoundTrips() {
        TenantContextHolder.set(TenantContext.of("acme"));
        assertThat(TenantContextHolder.getTenantId()).contains("acme");
        assertThat(TenantContextHolder.require().tenantId()).isEqualTo("acme");
    }

    @Test
    void getIsEmptyWhenUnset() {
        assertThat(TenantContextHolder.get()).isEmpty();
        assertThat(TenantContextHolder.getTenantId()).isEmpty();
    }

    @Test
    void requireThrowsWhenUnset() {
        assertThatThrownBy(TenantContextHolder::require)
                .isInstanceOf(TenantContextMissingException.class);
    }

    @Test
    void clearRemovesContext() {
        TenantContextHolder.set(TenantContext.of("acme"));
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void callWithRestoresPreviousContextOnNesting() {
        TenantContextHolder.set(TenantContext.of("acme"));

        String inner = TenantContextHolder.callWith(TenantContext.of("beta"),
                () -> TenantContextHolder.requireTenantId());

        assertThat(inner).isEqualTo("beta");
        assertThat(TenantContextHolder.requireTenantId()).isEqualTo("acme");
    }

    @Test
    void callWithRestoresEmptyContextWhenNoneBound() {
        TenantContextHolder.runWith(TenantContext.of("beta"),
                () -> assertThat(TenantContextHolder.requireTenantId()).isEqualTo("beta"));

        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void blankTenantIdRejected() {
        assertThatThrownBy(() -> TenantContext.of("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankOrgNormalizedToNull() {
        assertThat(new TenantContext("acme", "   ").org()).isNull();
    }
}
