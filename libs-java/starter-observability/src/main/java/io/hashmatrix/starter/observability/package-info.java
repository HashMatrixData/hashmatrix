/**
 * 可观测 starter：actuator 健康探针 + Micrometer/Prometheus 指标出口 + 平台公共标签。
 *
 * <p>依赖即获 {@code /actuator/health} 与 {@code /actuator/prometheus}；
 * {@link io.hashmatrix.starter.observability.ObservabilityAutoConfiguration} 注入统一指标标签。
 * 分布式链路（OpenTelemetry）经部署期 Java agent 自动注入，不绑应用代码（见架构 可观测决策）。
 */
package io.hashmatrix.starter.observability;
