package io.hashmatrix.starter.logging;

import io.hashmatrix.starter.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求级 MDC 注入：把当前租户与请求标识写入 {@link MDC}，请求结束后清理。
 *
 * <p>日志模板引用 {@code %X{tenantId}} / {@code %X{requestId}} 即可让每行日志带租户与请求维度，
 * 便于在 Loki/ES 按租户检索、并与审计（{@code starter-audit}）和链路（{@code starter-observability}）关联。
 * 请求标识沿用入站 {@code X-Request-Id}（跨服务透传），缺失则生成并回写响应头。
 */
public class MdcContextFilter extends OncePerRequestFilter {

    private final LoggingProperties properties;

    public MdcContextFilter(LoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestId = request.getHeader(properties.getRequestIdHeader());
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(properties.getRequestIdHeader(), requestId);

        MDC.put(properties.getRequestIdMdcKey(), requestId);
        TenantContextHolder.getTenantId()
                .ifPresent(tenantId -> MDC.put(properties.getTenantMdcKey(), tenantId));
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(properties.getRequestIdMdcKey());
            MDC.remove(properties.getTenantMdcKey());
        }
    }
}
