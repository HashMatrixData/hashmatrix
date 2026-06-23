# deploy/charts/platform-common —— 平台公共服务子 chart（M1 真实部署）

> **状态：真实（M1）**。`version: 0.1.0`，由 umbrella chart `platform` 按 `platform-common.enabled` 聚合。
> 关联：M1 贯通主线（I5 · platform-common 真实部署）。

## 定位

主仓 umbrella chart `platform` 下的 **platform-common** 子 chart（调度/工作流/统一元数据/审计等平台公共能力）。**D5：子仓 `services/platform-common` 交付 image，主仓 owns chart。**

- 事实源（镜像 + 应用契约）：`services/platform-common`（Java / Spring Boot，纯 JDBC 无 JPA），镜像 `ghcr.io/hashmatrixdata/platform-common`。
- 聚合方式：umbrella `platform/Chart.yaml` 以 `file://../platform-common` 声明依赖，按 `platform-common.enabled` 条件渲染（默认关，仅 `values-localdev` 开）。
- 分环境差异（prod/test/信创 xc）经 umbrella 的 `values-<env>.yaml` 注入，本子 chart 不内置环境耦合。

## M1 渲染内容

- **Deployment + Service `platform-common`**（固定 Service 名，供 gateway upstream `platform-common:8089` 解析）：应用 8089 / 管理 9089（actuator 独立端口）。
- **datasource → infra-dev in-cluster PG**：独立 db `platformcommon`（infra-dev initdb 幂等建），纯 JDBC/HikariCP。应用 env 为完整 JDBC URL（`PLATFORM_DB_URL`），模板按 `datasource.{host,port,db}` 拼接；HikariCP `initializationFailTimeout=-1`（应用侧）使 PG 缺失不阻塞启动、由健康探针反映真实状态。
- **readiness/liveness 探针**指向管理端口 9089（readiness deps-optional，仅 `readinessState`）。
- 资源限额 + JVM 堆上限（`MaxRAMPercentage`，受容器 limit 约束，防多 JVM 叠加 OOM）。

## 经网关可达

gateway chart 已加 `platform-common-upstream`（`platform-common:8089`）+ 受保护路由 `/api/platform/*`（共享 `auth-tenant`：OIDC 校验 → 注入 `X-Tenant-*` → 审计，**无 proxy-rewrite**，后端服务全路径 `/api/platform/...`）。

## 红线

仅 dev 占位凭据 / 脱敏 demo（`acme` / `tenant-demo` / `example.com`）；镜像 tag 固定（不用 `latest`）；生产经 ESO/secretRef 注入 `PLATFORM_DB_PASSWORD`。
