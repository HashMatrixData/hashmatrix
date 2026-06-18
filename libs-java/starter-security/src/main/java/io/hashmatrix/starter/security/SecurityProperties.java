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

    /** 免认证放行的路径（探针/指标等）。 */
    private List<String> permitPaths =
            new ArrayList<>(
                    List.of("/actuator/health", "/actuator/info", "/actuator/prometheus"));

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
