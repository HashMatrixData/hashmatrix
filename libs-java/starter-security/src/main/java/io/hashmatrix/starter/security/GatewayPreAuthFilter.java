package io.hashmatrix.starter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 网关预认证过滤器：信任网关（已完成 OIDC 校验）下发的身份/角色头，构建 {@link SecurityContext}。
 *
 * <p>应用不再二次校验 token（应用无感）；仅据 {@code X-User} / {@code X-Roles} 还原认证主体与权限，
 * 供方法级授权（{@code @PreAuthorize}）与 URL 授权使用。请求结束清理上下文，避免污染线程池线程。
 *
 * <p>注意：跨租户越权由数据层结合 {@code starter-tenant} 兜底，本过滤器只解决「身份与角色」。
 */
public class GatewayPreAuthFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;

    public GatewayPreAuthFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String user = request.getHeader(properties.getUserHeader());
        if (StringUtils.hasText(user)) {
            List<SimpleGrantedAuthority> authorities =
                    parseRoles(request.getHeader(properties.getRolesHeader()));
            PreAuthenticatedAuthenticationToken authentication =
                    new PreAuthenticatedAuthenticationToken(user, "N/A", authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<SimpleGrantedAuthority> parseRoles(String header) {
        if (!StringUtils.hasText(header)) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> new SimpleGrantedAuthority(properties.getRolePrefix() + role))
                .toList();
    }
}
