/**
 * 审计 starter：统一审计事件模型与记录器。
 *
 * <p>{@link io.hashmatrix.starter.audit.AuditEvent} 自动加盖当前租户上下文；
 * {@link io.hashmatrix.starter.audit.AuditRecorder} 默认走 slf4j 结构化输出，可被覆盖。
 * 分类分级/安全标签裁剪由 {@code security} 负责，本 starter 只提供通用审计基座。
 */
package io.hashmatrix.starter.audit;
