package io.hashmatrix.test.fixtures;

import java.util.List;

/**
 * 公共测试租户占位（全部脱敏，禁用任何真实甲方标识）。
 *
 * <p>多租户测试统一从此处取租户 id，保证各子仓 fixtures 一致：{@code acme} / {@code beta} 代表
 * 企业租户，{@code tenant-demo} 代表演示租户。对应架构 05「org = 租户」模型。
 *
 * @see MockData
 */
public final class MockTenants {

    /** 主用企业租户占位。 */
    public static final String ACME = "acme";

    /** 次用企业租户占位（跨租户隔离用例）。 */
    public static final String BETA = "beta";

    /** 演示/默认租户占位。 */
    public static final String TENANT_DEMO = "tenant-demo";

    private MockTenants() {
        throw new AssertionError("no instances");
    }

    /** 全部内置租户占位（稳定顺序，便于参数化用例）。 */
    public static List<String> all() {
        return List.of(ACME, BETA, TENANT_DEMO);
    }
}
