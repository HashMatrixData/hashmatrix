package io.hashmatrix.starter.audit;

import io.hashmatrix.starter.tenant.TenantContextHolder;
import java.time.Instant;

/**
 * 不可变审计事件：记录「谁 / 何时 / 在哪个租户 / 对什么 / 做了什么 / 结果如何」。
 *
 * <p>{@code tenantId} 自动取自当前 {@link TenantContextHolder}（见架构 05 §5）；跨租户审计绝不串。
 * 安全标签/分类分级裁剪由 {@code security} 负责，本模型只承载结构化审计要素，不落具体业务明细。
 *
 * @param timestamp 事件时刻
 * @param tenantId  租户标识；当前线程无上下文时为 {@code null}
 * @param actor     操作者（账号/服务标识，脱敏）
 * @param action    动作（如 {@code LOGIN} / {@code PUBLISH} / {@code DELETE}）
 * @param target    操作对象（如资源标识）
 * @param outcome   结果
 * @param detail    可选补充（不得含凭据/客户可识别信息）
 */
public record AuditEvent(
        Instant timestamp,
        String tenantId,
        String actor,
        String action,
        String target,
        Outcome outcome,
        String detail) {

    /** 审计结果。 */
    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    /**
     * 以「当前时刻 + 当前租户上下文」构造审计事件。
     *
     * @return 已加盖时间与租户的审计事件
     */
    public static AuditEvent of(
            String actor, String action, String target, Outcome outcome, String detail) {
        String tenantId = TenantContextHolder.getTenantId().orElse(null);
        return new AuditEvent(Instant.now(), tenantId, actor, action, target, outcome, detail);
    }
}
