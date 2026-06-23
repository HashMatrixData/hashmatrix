package io.hashmatrix.starter.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用侧鉴权 starter 配置项，前缀 {@code hashmatrix.security}。
 */
@ConfigurationProperties("hashmatrix.security")
public class SecurityProperties {

    /** 是否启用默认安全过滤链与预认证（默认启用）。 */
    private boolean enabled = true;

    /** 网关下发的用户标识请求头。 */
    private String userHeader = "X-User";

    /** 网关下发的角色请求头（逗号分隔）。 */
    private String rolesHeader = "X-Roles";

    /** 角色权限前缀（与 Spring Security 约定一致）。 */
    private String rolePrefix = "ROLE_";

    /**
     * 免认证放行的路径（探针/指标等）。
     *
     * <p>含 {@code /actuator/health/**} 通配：{@code requestMatchers("/actuator/health")} 仅<b>精确</b>匹配，
     * 不覆盖 K8s 存活/就绪探针访问的 {@code /actuator/health/liveness}、{@code /actuator/health/readiness}
     * 子路径——缺通配则匿名探针落 {@code anyRequest().authenticated()} 被 401，readiness 永不转绿（hashmatrix#26）。
     * {@code /**} 在 AntPath/PathPattern 下同时覆盖 {@code /actuator/health} 本身。
     *
     * <p><b>部署期注意</b>：放行 health 子端点后，若叠加 {@code management.endpoint.health.show-details: always}，
     * 会把 db/flowable 等组件级明细暴露给未认证调用方；建议各服务收敛为 {@code when_authorized}。
     */
    private List<String> permitPaths =
            new ArrayList<>(
                    List.of(
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info",
                            "/actuator/prometheus"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserHeader() {
        return userHeader;
    }

    public void setUserHeader(String userHeader) {
        this.userHeader = userHeader;
    }

    public String getRolesHeader() {
        return rolesHeader;
    }

    public void setRolesHeader(String rolesHeader) {
        this.rolesHeader = rolesHeader;
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public List<String> getPermitPaths() {
        return permitPaths;
    }

    public void setPermitPaths(List<String> permitPaths) {
        this.permitPaths = permitPaths;
    }
}
