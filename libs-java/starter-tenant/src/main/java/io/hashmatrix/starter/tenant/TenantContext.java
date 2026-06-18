package io.hashmatrix.starter.tenant;

/**
 * 不可变租户上下文：标识当前请求/执行流所属租户。
 *
 * <p>{@code tenantId} = Keycloak Organization（org）声明，公网 SaaS 下为企业、私有化下为部门
 * （见架构 05 §1/§5）。{@code org} 为可选的原始 org 标识，缺省与 {@code tenantId} 同义，留作演进。
 *
 * @param tenantId 租户标识，非空白
 * @param org      可选原始 org 标识；空白归一化为 {@code null}
 */
public record TenantContext(String tenantId, String org) {

    public TenantContext {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        tenantId = tenantId.trim();
        org = (org == null || org.isBlank()) ? null : org.trim();
    }

    /** 仅有租户标识的上下文。 */
    public static TenantContext of(String tenantId) {
        return new TenantContext(tenantId, null);
    }
}
