/**
 * 多租户上下文 starter。
 *
 * <p>链路：网关校验 JWT 并注入 {@code X-Tenant-*} 头 → {@link io.hashmatrix.starter.tenant.TenantContextFilter}
 * 解析为 {@link io.hashmatrix.starter.tenant.TenantContext} 并绑定
 * {@link io.hashmatrix.starter.tenant.TenantContextHolder} → 数据访问层路由 schema/catalog + 行级兜底。
 * 详见架构 05《多租户与控制平面》§5。
 */
package io.hashmatrix.starter.tenant;
