# libs-java Changelog

## libs-java-v0.2.2 — 2026-06-23

探针放行修复（K8s 存活/就绪探针被 401）；消费方应直接升至 0.2.2（hashmatrix#26）。

- `hashmatrix-starter-security`：默认 `permitPaths` 增列 `/actuator/health/**`。`requestMatchers("/actuator/health")` 在 Spring Security 下**仅精确匹配**，不覆盖 health group 子端点；导致匿名 K8s 探针访问 `/actuator/health/{liveness,readiness}` 落入 `anyRequest().authenticated()` 被拒成 401。`/**` 在 AntPath/PathPattern 下同时覆盖 `/actuator/health` 本身。`SecurityFilterChainWebMvcTest` 的 `@ValueSource` 增列两条探针子路径钉死根因，退回精确路径即报红。

## libs-java-v0.2.1 — 2026-06-19

生产装配修复（真实部署 / 重打包场景下公共能力失效的修正）；消费方应直接升至 0.2.1。

- `hashmatrix-starter-security`：修正 `SecurityFilterChainConfiguration` 自动装配顺序（`after`→`before`），否则生产环境网关预认证过滤链整体失效、退化为 Spring Boot 默认安全（CSRF 开、`X-Roles` 被忽略、无 401 入口）。
- `hashmatrix-starter-security`：新增 `SecurityErrorAdvice`——方法级 `@PreAuthorize` 拒绝映射为 403，避免被应用兜底吞成 500。
- `hashmatrix-starter-web`：`GlobalExceptionHandler` 对实现 `ErrorResponse` 的框架异常按其携带状态返回（如 `NoResourceFound` → 404，不再一律 500）。

## libs-java-v0.2.0 — 2026-06-18

补齐 spec §3 公共能力「日志 / 审计 / 鉴权」，公共依赖达到承诺线。

- `hashmatrix-starter-audit`：审计基座——`AuditEvent` + `AuditRecorder`（默认 slf4j 结构化），事件自动加盖当前租户（取 `TenantContextHolder`），`@ConditionalOnMissingBean` 可覆盖。配置前缀 `hashmatrix.audit.*`。
- `hashmatrix-starter-observability`：actuator 健康探针 + `/actuator/prometheus` 指标出口 + 公共指标标签（`MeterRegistryCustomizer`）；OTel 链路走部署期 Java agent。配置前缀 `hashmatrix.observability.*`。
- `hashmatrix-starter-logging`：请求级 MDC 注入 `tenantId`/`requestId`（与审计/链路三方关联），沿用/生成 `X-Request-Id`。配置前缀 `hashmatrix.logging.*`。
- `hashmatrix-starter-security`：应用侧鉴权——信任网关下发 `X-User`/`X-Roles`（应用无感），`GatewayPreAuthFilter` 建 SecurityContext + 无状态默认过滤链 + `@PreAuthorize` 方法级授权。配置前缀 `hashmatrix.security.*`。
- 四个 starter 均纳入 `hashmatrix-bom` 统一版本管理；reactor 全量构建 + 单测通过。

## libs-java-v0.1.0 — 2026-06-18

首个公共依赖基线（Java 17 · Spring Boot 3.3.5），对应 GitHub Issue #1。

- `hashmatrix-platform-parent`：统一 Java 版本 / 插件管理 / enforcer 质量门 / profile（oss·信创·release）/ GitHub Packages 发布配置。
- `hashmatrix-bom`：钉死 Spring Boot 家族 + Testcontainers + `starter-*` 版本——开发框架版本唯一来源。
- `hashmatrix-starter-tenant`：多租户上下文 `TenantContext`（X-Tenant-* 头 → ThreadLocal，呼应架构 05 §5）。
- `hashmatrix-starter-web`：统一返回 `ApiResponse` + 全局异常处理 `GlobalExceptionHandler`。
- `hashmatrix-starter-test`：JUnit5 + AssertJ + Mockito + Testcontainers 统一测试栈 + 脱敏 fixtures（`MockTenants`/`MockData`）。
- 子仓经 Maven 坐标（`<parent>` + import BOM）引用，验证「只 clone 子仓可构建」；CI 发布到 GitHub Packages，附内网私服镜像同步流程。
