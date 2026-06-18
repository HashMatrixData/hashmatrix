package io.hashmatrix.starter.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class GatewayPreAuthFilterTest {

    private final GatewayPreAuthFilter filter = new GatewayPreAuthFilter(new SecurityProperties());

    @Test
    void buildsAuthenticationFromGatewayHeadersThenClears() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User", "acme-user");
        request.addHeader("X-Roles", "admin, viewer");

        List<String> rolesDuringChain = new ArrayList<>();
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) ->
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .forEach(rolesDuringChain::add));

        assertThat(rolesDuringChain).containsExactlyInAnyOrder("ROLE_admin", "ROLE_viewer");
        // 请求结束后上下文必须清理
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void leavesContextUnauthenticatedWhenNoUserHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        boolean[] authPresent = {true};
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) ->
                        authPresent[0] =
                                SecurityContextHolder.getContext().getAuthentication() != null);
        assertThat(authPresent[0]).isFalse();
    }
}
