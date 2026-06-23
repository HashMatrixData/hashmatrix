package io.hashmatrix.starter.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * starter-security 默认过滤链 + 方法安全的 MockMvc 切片测试（主仓 {@code hashmatrix#24}，源
 * {@code control-plane#10}）—— 在 <b>starter 层</b>为平台鉴权基线建立独立门禁。
 *
 * <p><b>守护什么</b>：以 starter 的<b>真实自动装配</b>（{@link SecurityAutoConfiguration} +
 * {@link SecurityFilterChainConfiguration}，经 {@code @EnableAutoConfiguration} 按生产机制加载）驱动一次
 * 真实 {@code DispatcherServlet} + servlet 过滤链，断言平台基线三态：<b>匿名 → 401</b>（过滤链入口
 * {@link org.springframework.security.web.authentication.HttpStatusEntryPoint}）、<b>已认证缺角色 → 403</b>
 * （方法级 {@code @PreAuthorize}）、<b>{@code permitPaths}（探针）→ 放行</b>，以及 <b>SUPERADMIN → 放行</b>。
 *
 * <p><b>相对 control-plane 切片的价值</b>：{@code TenantApiSecurityTest}（消费方）由<b>测试类自带</b>
 * {@code @EnableMethodSecurity}——非生产装配；本测试<b>不</b>自带，方法安全完全由 starter 的
 * {@link SecurityFilterChainConfiguration} 开启，故守护的是「starter 默认链<b>本身</b>是否成立」。一旦该链
 * 漂移（如 401 入口丢失、方法安全未开），本测试在 starter 层即报警，不再仅靠消费方间接发现。
 *
 * <p><b>{@link SecurityErrorAdvice} 回归守护</b>：上下文故意装配一个兜底 {@link SwallowAllAdvice}
 * （{@code @ExceptionHandler(Exception.class) → 500}，模拟 starter-web {@code GlobalExceptionHandler}）。
 * 「已认证缺角色 → 403」用例在该兜底在场时仍须得 403——证明 starter 的最高优先级 {@code SecurityErrorAdvice}
 * 抢先渲染 403，方法级拒绝不会被吞成 500（WP5 回归在 starter 层独立守护）。
 *
 * <p><b>探针覆盖边界</b>：本上下文未挂载 actuator（与「安全链是否放行」正交），故探针落 404 而非 200；
 * 断言「非 401/403」即精确证明放行，且不耦合 management 端口拓扑。探针在生产端口的真实 200 可达性由部署期
 * readiness/冒烟覆盖。
 */
@SpringBootTest(
        classes = SecurityFilterChainWebMvcTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityFilterChainWebMvcTest {

    /** 仅需已认证（{@code authenticated()}）的只读端点。 */
    private static final String OPEN = "/api/open";

    /** 高危端点（方法级 {@code @PreAuthorize("hasRole('SUPERADMIN')")}）。 */
    private static final String HIGH_RISK = "/api/high-risk";

    @Autowired private MockMvc mvc;

    @Test
    void anonymousReadIsUnauthorized() throws Exception {
        // 无网关身份头：anyRequest().authenticated() + 401 入口，在进入控制器前即拒。
        mvc.perform(get(OPEN)).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousHighRiskIsUnauthorized() throws Exception {
        mvc.perform(post(HIGH_RISK)).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedNonSuperadminIsForbiddenNotSwallowedTo500() throws Exception {
        // 已认证但角色不足（USER）→ 方法级 @PreAuthorize 在控制器体前拒。兜底 500 advice 在场，
        // 仍须得 403 —— 证明最高优先级 SecurityErrorAdvice 抢先渲染，未被吞成 500。
        mvc.perform(post(HIGH_RISK).header("X-User", "alice").header("X-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanReadOpenEndpoint() throws Exception {
        // 只读端点仅需已认证（USER 即可）→ 200。
        // 注：刻意以「授权结果」而非读取安全上下文来断言——网关预认证为<b>无状态</b>，GatewayPreAuthFilter
        // 直接写 SecurityContextHolder、不经 SecurityContextRepository 持久化（故 spring-security-test 的
        // authenticated() matcher 读不到、不适用，本仓亦不引入该依赖）。ROLE_ 权限是否按 X-Roles 正确构建，
        // 已由下方用例的授权结果严格反证：USER 在高危端点得 403、SUPERADMIN 得 200。
        mvc.perform(get(OPEN).header("X-User", "alice").header("X-Roles", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void superadminIsAllowedOnHighRisk() throws Exception {
        // SUPERADMIN → 高危端点放行（方法安全 hasRole('SUPERADMIN') 通过）→ 200。
        mvc.perform(post(HIGH_RISK).header("X-User", "ops").header("X-Roles", "SUPERADMIN"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "探针放行(非 401/403): {0}")
    @ValueSource(
            strings = {
                "/actuator/health",
                // K8s 存活/就绪探针子路径——精确路径放行不覆盖，须靠 /actuator/health/** 通配（hashmatrix#26）。
                // 若默认 permitPaths 退回精确路径，这两条立即被 anyRequest().authenticated() 拒成 401，本测试报警。
                "/actuator/health/liveness",
                "/actuator/health/readiness",
                "/actuator/info",
                "/actuator/prometheus"
            })
    void defaultPermitPathsAreNotBlockedBySecurity(String path) throws Exception {
        // 唯一关切：默认 permitPaths 被安全链<b>放行</b>（匿名访问未被 401/403 拦）。
        // 本上下文无 actuator、无该资源处理器，故请求穿过安全链后在 MVC 层的最终状态码（无处理器 404、
        // 或被上面的兜底 SwallowAllAdvice 吞成 500）属 MVC 层行为，与「是否放行」正交——刻意只断言「非
        // 401/403」、不收紧为状态白名单，避免把「安全是否放行」的语义错误耦合到「无处理器」的渲染行为。
        // 若 permitPaths 失效，匿名访问会被 anyRequest().authenticated() 拒成 401，本断言立即失败。
        int sc = mvc.perform(get(path)).andReturn().getResponse().getStatus();
        assertTrue(
                sc != 401 && sc != 403,
                () -> "探针须被安全链放行（非 401/403），实际=" + sc + " path=" + path);
    }

    /**
     * 最小测试应用：仅 {@code @EnableAutoConfiguration}，让 starter 的两个自动装配按<b>真实机制</b>加载
     * （含 {@code SecurityFilterChainConfiguration} 的 {@code @EnableMethodSecurity} 与默认过滤链），
     * 不在测试侧重复声明任何安全开关——这是本测试守护「starter 自身链成立」的前提。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {

        @Bean
        ProtectedController protectedController() {
            return new ProtectedController();
        }

        /** 模拟应用兜底全局异常处理（如 starter-web {@code GlobalExceptionHandler}）：未经治理会把安全拒绝吞成 500。 */
        @Bean
        SwallowAllAdvice swallowAllAdvice() {
            return new SwallowAllAdvice();
        }
    }

    /** 测试用控制器：一个仅需认证的只读端点 + 一个 SUPERADMIN 高危端点。 */
    @RestController
    static class ProtectedController {

        @GetMapping(OPEN)
        String open() {
            return "ok";
        }

        @PostMapping(HIGH_RISK)
        @PreAuthorize("hasRole('SUPERADMIN')")
        String highRisk() {
            return "ok";
        }
    }

    /** 兜底全局异常 advice（默认 {@code LOWEST_PRECEDENCE}）：任何异常 → 500，用于反衬 SecurityErrorAdvice 抢先。 */
    @RestControllerAdvice
    static class SwallowAllAdvice {

        @ExceptionHandler(Exception.class)
        ResponseEntity<String> any(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("swallowed-to-500");
        }
    }
}
