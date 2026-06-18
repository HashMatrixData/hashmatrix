package io.hashmatrix.test.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** 校验脱敏 Mock 工具：确定性序号 + 红线占位（@example.com / *.example.internal）。 */
class MockDataTest {

    @Test
    void emailUsesDesensitizedDomain() {
        assertThat(MockData.email("alice")).isEqualTo("alice@example.com");
    }

    @Test
    void hostUsesDesensitizedInternalDomain() {
        assertThat(MockData.host("nexus")).isEqualTo("nexus.example.internal");
    }

    @Test
    void sequenceIsStrictlyMonotonic() {
        long first = MockData.nextSeq();
        long second = MockData.nextSeq();
        assertThat(first).isPositive();
        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void sampleUserIsDesensitizedAndBelongsToTenant() {
        Map<String, Object> user = MockData.sampleUser(MockTenants.ACME);
        assertThat(user).containsKeys("id", "tenant", "username", "email");
        assertThat(user.get("tenant")).isEqualTo("acme");
        assertThat((String) user.get("email")).endsWith("@example.com");
    }

    @Test
    void sampleUserDefaultsToDemoTenant() {
        assertThat(MockData.sampleUser().get("tenant")).isEqualTo(MockTenants.TENANT_DEMO);
    }

    @Test
    void builtInTenantsAreLowercasePlaceholders() {
        assertThat(MockTenants.all())
                .containsExactly("acme", "beta", "tenant-demo")
                .allSatisfy(id -> assertThat(id).isEqualTo(id.toLowerCase()));
    }
}
