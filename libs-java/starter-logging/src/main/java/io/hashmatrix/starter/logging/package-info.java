/**
 * 日志关联 starter：请求级 MDC 注入 tenantId / requestId。
 *
 * <p>{@link io.hashmatrix.starter.logging.MdcContextFilter} 在请求进入时把当前租户与请求标识写入
 * MDC、结束时清理；日志模板引用 {@code %X{tenantId}} / {@code %X{requestId}} 即获租户与请求维度，
 * 与审计（{@code starter-audit}）、链路（{@code starter-observability}）形成三方关联。
 * 具体编码格式（JSON/ECS）由部署期日志配置决定，本 starter 不绑定编码器。
 */
package io.hashmatrix.starter.logging;
