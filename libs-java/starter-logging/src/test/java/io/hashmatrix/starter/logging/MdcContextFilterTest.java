package io.hashmatrix.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import io.hashmatrix.starter.tenant.TenantContext;
import io.hashmatrix.starter.tenant.TenantContextHolder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcContextFilterTest {

    private final MdcContextFilter filter = new MdcContextFilter(new LoggingProperties());

    @Test
    void putsTenantAndRequestIdIntoMdcDuringChainThenClears() throws Exception {
        Map<String, String> seen = new HashMap<>();
        TenantContextHolder.runWith(
                TenantContext.of("tenant-demo"),
                () -> {
                    MockHttpServletRequest request = new MockHttpServletRequest();
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    try {
                        filter.doFilter(
                                request,
                                response,
                                (req, res) -> {
                                    seen.put("tenantId", MDC.get("tenantId"));
                                    seen.put("requestId", MDC.get("requestId"));
                                });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    assertThat(seen.get("tenantId")).isEqualTo("tenant-demo");
                    assertThat(seen.get("requestId")).isNotBlank();
                    // 响应回写请求标识，便于跨服务透传
                    assertThat(response.getHeader("X-Request-Id")).isEqualTo(seen.get("requestId"));
                });
        // 请求结束后 MDC 必须清理，避免污染线程池中复用线程
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void reusesInboundRequestId() throws Exception {
        Map<String, String> seen = new HashMap<>();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-123");
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> seen.put("requestId", MDC.get("requestId")));
        assertThat(seen.get("requestId")).isEqualTo("req-123");
    }
}
