/**
 * 应用侧鉴权 starter：信任网关下发身份/角色，构建 SecurityContext + 方法级授权。
 *
 * <p>认证（OIDC 校验）在网关（APISIX + Keycloak）完成，应用无感；
 * {@link io.hashmatrix.starter.security.GatewayPreAuthFilter} 据 {@code X-User}/{@code X-Roles} 还原主体与权限，
 * {@link io.hashmatrix.starter.security.SecurityFilterChainConfiguration} 提供无状态默认过滤链并开启
 * {@code @PreAuthorize}。跨租户越权由数据层结合 {@code starter-tenant} 兜底。
 */
package io.hashmatrix.starter.security;
