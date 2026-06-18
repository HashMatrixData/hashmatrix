package io.hashmatrix.starter.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextFilterTest {

    private final TenantProperties properties = new TenantProperties();
    private final TenantContextFilter filter = new TenantContextFilter(properties);

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void bindsTenantDuringChainAndClearsAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "acme");
        request.addHeader("X-Tenant-Org", "acme-org");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<TenantContext> seenInChain = new AtomicReference<>();
        FilterChain capturing = (req, res) -> seenInChain.set(TenantContextHolder.require());

        filter.doFilter(request, response, capturing);

        assertThat(seenInChain.get().tenantId()).isEqualTo("acme");
        assertThat(seenInChain.get().org()).isEqualTo("acme-org");
        assertThat(TenantContextHolder.get()).as("cleared after request").isEmpty();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void clearsContextEvenWhenChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain boom = (req, res) -> {
            throw new ServletException("boom");
        };

        try {
            filter.doFilter(request, response, boom);
        } catch (ServletException | IOException expected) {
            // ignored: asserting cleanup, not propagation
        }

        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void passesThroughWhenHeaderAbsentAndNotRequired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).as("chain proceeded").isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void rejectsWithBadRequestWhenHeaderAbsentAndRequired() throws Exception {
        properties.setRequired(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).as("chain short-circuited").isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("TENANT_REQUIRED");
    }

    @Test
    void honoursCustomHeaderName() throws Exception {
        properties.setHeader("X-Org");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Org", "beta");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(TenantContextHolder.requireTenantId());

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isEqualTo("beta");
    }
}
