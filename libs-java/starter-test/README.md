# hashmatrix-starter-test

统一测试栈：以 `<scope>test</scope>` 引入即获 **JUnit5 + AssertJ + Mockito + Spring Test + Testcontainers**
（`spring-boot-starter-test` + `testcontainers/junit-jupiter` + `postgresql`）+ 脱敏 Mock fixtures（`io.hashmatrix.test.fixtures.*`）。

```xml
<dependency>
  <groupId>io.hashmatrix</groupId>
  <artifactId>hashmatrix-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

## 🔴 用法红线：`@SpringBootTest` 必须能发现一个 `@SpringBootConfiguration`

> 源：governance→主仓 Discussion #21（`InfraConnectivityIT` bootstrap 失败）。**这是用法问题、不是平台 bug。**

`@SpringBootTest` 引导需要一个 `@SpringBootConfiguration`（通常即 `@SpringBootApplication` 主类）。**约束**：

1. 子仓**必须有**一个 `@SpringBootApplication` 主类（如 `io.hashmatrix.<svc>.<Svc>Application`）；
2. 该主类**必须位于集成测试所在包的祖先包**——Spring Boot 从测试类所在包**向上**搜 `@SpringBootConfiguration`，
   搜不到就失败。把主类放在根包（`io.hashmatrix.<svc>`）、IT 放其子包（`io.hashmatrix.<svc>.xxx`）即满足；
3. 结构对了就**用裸 `@SpringBootTest`**，**不要写 `@SpringBootTest(classes=...)`** 去硬指——`classes=` 指错类反而触发
   `Failed to find merged annotation for @BootstrapWith`。

**两个常见报错（同一根因的两面）**：
- `Unable to find a @SpringBootConfiguration` —— 没有主类 / 主类不在 IT 的祖先包。
- `Failed to find merged annotation for @org.springframework.test.context.BootstrapWith(...)` —— `classes=` 指向了非配置类。

### Canonical 模板：`examples/sample-service`

主仓 `libs-java/examples/sample-service` 是**已验证绿**的 `@SpringBootTest` 样板（`SampleApplicationTests` 4/4 通过）：
- `SampleApplication`（根包 `io.hashmatrix.examples.sample`，仅 `@SpringBootApplication` + `main`）；
- `SampleApplicationTests`（同包，`@SpringBootTest @AutoConfigureMockMvc`，断言 starter 装配 + MockMvc）。

**照抄它的结构即可**；若照抄后仍红，附最小复现（主类位置 + IT 包名 + 完整栈）上抛主仓 / libs-java。

## fixtures

`io.hashmatrix.test.fixtures` 提供脱敏 Mock 数据与租户占位（`MockData` / `MockTenants`，如 `MockTenants.ACME`）。
**红线**：测试数据一律虚构脱敏（`acme` / `tenant-demo` / `example.com`），禁用任何真实甲方数据。
