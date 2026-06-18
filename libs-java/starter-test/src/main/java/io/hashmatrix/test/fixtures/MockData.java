package io.hashmatrix.test.fixtures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 脱敏 Mock 数据工具：生成虚构、可重复的测试数据。
 *
 * <p><b>红线约定</b>：本工具产出的所有数据必须为虚构占位——邮箱统一 {@code @example.com}，
 * 主机用 {@code *.example.internal}，租户取 {@link MockTenants}。禁止在 fixtures 中出现任何
 * 真实甲方可识别信息（见主仓 {@code CLAUDE.md} 信息红线）。
 *
 * <p>序号在进程内单调自增（首次调用返回 1），不依赖随机源。注意是<b>进程级</b>共享计数器：
 * 同一 JVM 内跨用例累加，故勿用具体序号值做快照断言，只可依赖「单调、确定性顺序」。
 */
public final class MockData {

    /** 脱敏邮箱域。 */
    public static final String EMAIL_DOMAIN = "example.com";

    /** 脱敏内网主机域（信创/内网示例用）。 */
    public static final String HOST_DOMAIN = "example.internal";

    private static final AtomicLong SEQ = new AtomicLong(0);

    private MockData() {
        throw new AssertionError("no instances");
    }

    /** 下一个进程内自增序号（单调递增；进程首次调用返回 1）。 */
    public static long nextSeq() {
        return SEQ.incrementAndGet();
    }

    /** 脱敏邮箱：{@code <local>@example.com}。 */
    public static String email(String local) {
        return local + "@" + EMAIL_DOMAIN;
    }

    /** 脱敏用户名：{@code user-<seq>}。 */
    public static String username() {
        return "user-" + nextSeq();
    }

    /** 脱敏内网主机名：{@code <name>.example.internal}。 */
    public static String host(String name) {
        return name + "." + HOST_DOMAIN;
    }

    /**
     * 一条脱敏用户样例（隶属指定租户）。
     *
     * @param tenant 租户占位（建议取自 {@link MockTenants}）
     * @return 有序、虚构的用户记录
     */
    public static Map<String, Object> sampleUser(String tenant) {
        long seq = nextSeq();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", seq);
        user.put("tenant", tenant);
        user.put("username", "user-" + seq);
        user.put("email", email("user-" + seq));
        return user;
    }

    /** 默认租户（{@link MockTenants#TENANT_DEMO}）下的一条脱敏用户样例。 */
    public static Map<String, Object> sampleUser() {
        return sampleUser(MockTenants.TENANT_DEMO);
    }
}
