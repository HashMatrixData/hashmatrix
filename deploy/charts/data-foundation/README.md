# deploy/charts/data-foundation —— 数据底座分系统子 chart（M1 真实部署）

> **状态：真实（M1）**。`version: 0.1.0`，由 umbrella chart `platform` 按 `data-foundation.enabled` 聚合。
> 关联：M1 贯通主线（I5 · data-foundation 真实部署）。

## 定位

主仓 umbrella chart `platform` 下的 **data-foundation** 子 chart。**D5：子仓 `services/data-foundation` 交付 image，主仓 owns chart。**

- 事实源（镜像 + 应用契约）：`services/data-foundation`（Java / Spring Boot，app 模块为运行入口），镜像 `ghcr.io/hashmatrixdata/data-foundation`。
- 聚合方式：umbrella `platform/Chart.yaml` 以 `file://../data-foundation` 声明依赖，按 `data-foundation.enabled` 条件渲染（默认关，仅 `values-localdev` 开）。
- 分环境差异（prod/test/信创 xc）经 umbrella 的 `values-<env>.yaml` 注入，本子 chart 不内置环境耦合。

## M1 渲染内容

- **Deployment + Service `data-foundation`**（固定 Service 名，供 gateway upstream `data-foundation:8084` 解析）：应用 8084 / 管理 9084（actuator 独立端口）。
- **M1 无持久 datasource**：app 模块为连接器目录骨架（`ConnectorController`），不连 PG → 本子 chart 不注入 PG env。采集/计算（Flink/CDC/SeaTunnel）属 post-M1，落地时再补 datasource/中间件接线。
- **readiness/liveness 探针**指向管理端口 9084（readiness deps-optional，仅 `readinessState`）。
- 资源限额 + JVM 堆上限（`MaxRAMPercentage`，受容器 limit 约束，防多 JVM 叠加 OOM）。

## 经网关可达

gateway chart 已加 `data-foundation-upstream`（`data-foundation:8084`）+ 受保护路由 `/api/connectors/*`（共享 `auth-tenant`：OIDC 校验 → 注入 `X-Tenant-*` → 审计，**无 proxy-rewrite**，后端服务全路径 `/api/connectors/...`）。

## 红线

仅 dev 占位凭据 / 脱敏 demo（`acme` / `tenant-demo` / `example.com`）；镜像 tag 固定（不用 `latest`）。
