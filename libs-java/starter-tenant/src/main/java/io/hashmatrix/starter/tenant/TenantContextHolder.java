package io.hashmatrix.starter.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 当前线程的租户上下文持有者（ThreadLocal）。
 *
 * <p>由 {@link TenantContextFilter} 在请求进入时绑定、请求结束时清理；数据访问层据此路由
 * schema/catalog 并做行级兜底过滤（架构 05 §5）。
 *
 * <p>跨线程（异步/线程池）须显式传播：用 {@link #callWith} / {@link #runWith} 在目标线程内
 * 临时绑定并保证还原，避免污染线程池中复用的线程。
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
        throw new AssertionError("no instances");
    }

    /** 绑定上下文到当前线程。 */
    public static void set(TenantContext context) {
        CONTEXT.set(Objects.requireNonNull(context, "context"));
    }

    /** 当前上下文（可能为空）。 */
    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /** 当前租户标识（可能为空）。 */
    public static Optional<String> getTenantId() {
        return get().map(TenantContext::tenantId);
    }

    /**
     * 强制取用当前上下文。
     *
     * @throws TenantContextMissingException 当前线程未绑定租户上下文
     */
    public static TenantContext require() {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            throw new TenantContextMissingException();
        }
        return context;
    }

    /** 强制取用当前租户标识。 */
    public static String requireTenantId() {
        return require().tenantId();
    }

    /** 清理当前线程上下文（务必在 finally 中调用）。 */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 在给定上下文下执行并返回结果，执行后还原既有上下文（支持嵌套）。
     *
     * @param context  临时绑定的上下文
     * @param supplier 业务逻辑
     * @return supplier 的返回值
     */
    public static <T> T callWith(TenantContext context, Supplier<T> supplier) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(supplier, "supplier");
        TenantContext previous = CONTEXT.get();
        CONTEXT.set(context);
        try {
            return supplier.get();
        } finally {
            if (previous != null) {
                CONTEXT.set(previous);
            } else {
                CONTEXT.remove();
            }
        }
    }

    /** {@link #callWith} 的无返回值版本。 */
    public static void runWith(TenantContext context, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        callWith(context, () -> {
            runnable.run();
            return null;
        });
    }
}
