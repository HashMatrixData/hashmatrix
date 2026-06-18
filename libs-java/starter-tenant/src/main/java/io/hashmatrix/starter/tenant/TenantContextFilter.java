package io.hashmatrix.starter.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从网关注入的请求头解析租户标识，绑定到 {@link TenantContextHolder}，请求结束清理。
 *
 * <p>缺头处理由 {@link TenantProperties#isRequired()} 决定：强制时以 400 拒绝，否则放行。
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final TenantProperties properties;

    public TenantContextFilter(TenantProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String tenantId = trimToNull(request.getHeader(properties.getHeader()));

        if (tenantId == null) {
            if (properties.isRequired()) {
                log.debug("Rejecting request without tenant header '{}'", properties.getHeader());
                writeMissingTenant(response);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String org = trimToNull(request.getHeader(properties.getOrgHeader()));
        try {
            TenantContextHolder.set(new TenantContext(tenantId, org));
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static void writeMissingTenant(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"TENANT_REQUIRED\",\"message\":\"Missing tenant header\"}");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
