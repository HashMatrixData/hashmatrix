package io.hashmatrix.starter.tenant;

/**
 * 在缺少租户上下文时强制取用（{@link TenantContextHolder#require()}）抛出。
 *
 * <p>通常意味着请求未经网关注入 {@code X-Tenant-*} 头，或在无租户上下文的线程中访问了
 * 租户隔离资源——属调用方编程/配置错误，不应被业务静默吞掉。
 */
public class TenantContextMissingException extends IllegalStateException {

    public TenantContextMissingException() {
        super("No tenant context bound to the current thread");
    }
}
