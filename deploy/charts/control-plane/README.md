# deploy/charts/control-plane —— 多租户控制平面子 chart（M1 真实部署）

> **状态：真实（M1）**。`version: 0.1.0`，由 umbrella chart `platform` 按 `control-plane.enabled` 聚合。
> 关联：M1 贯通主线（I5 · control-plane 真实部署）。

## 定位

主仓 umbrella chart `platform` 下的 **control-plane** 子 chart。**D5：子仓 `services/control-plane` 交付 image，主仓 owns chart。**

- 事实源（镜像 + 应用契约）：`services/control-plane`（Java / Spring Boot），镜像 `ghcr.io/hashmatrixdata/control-plane`。
- 聚合方式：umbrella `platform/Chart.yaml` 以 `file://../control-plane` 声明依赖，按 `control-plane.enabled` 条件渲染（默认关，仅 `values-localdev` 开）。
- 分环境差异（prod/test/信创 xc）经 umbrella 的 `values-<env>.yaml` 注入，本子 chart 不内置环境耦合。

## M1 渲染内容

- **Deployment + Service `control-plane`**（固定 Service 名，供 gateway upstream `control-plane:8081` 解析）：应用 8081 / 管理 9081（actuator 独立端口）。
- **datasource → infra-dev in-cluster PG**：独立 db `controlplane`（infra-dev initdb 幂等建），Flyway 管 schema。应用 env 为完整 JDBC URL（`CONTROL_PLANE_DB_URL`），模板按 `datasource.{host,port,db}` 拼接。
- **readiness/liveness 探针**指向管理端口 9081（readiness deps-optional：仅 `readinessState`，PG 缺失不拖垮探针）。
- 资源限额 + JVM 堆上限（`MaxRAMPercentage`，受容器 limit 约束，防多 JVM 叠加 OOM）。

## 身份开通（D6）

默认 `keycloak.enabled=false` → provisioner `identity=stub`（无需活 Keycloak，时序可跑通）。
置 `keycloak.enabled=true` 切真实 Keycloak Admin IdentityProvisioner（建 org + 租户管理员，回写 `keycloak_org_id`）；
经 Spring relaxed-binding 直绑 `hashmatrix.control-plane.provisioning.{identity,keycloak.*}`，集群内 `keycloak:8080`。
M1 D6：身份后端只做真实 OIDC 登录 + control-plane 真实 IdentityProvisioner，其余 provisioner（compute/data/secrets）仍 stub/后置。

## 经网关可达

gateway chart 已加 `control-plane-upstream`（`control-plane:8081`）+ 受保护路由 `/api/v1/*`（共享 `auth-tenant`：OIDC 校验 → 注入 `X-Tenant-*` → 审计，**无 proxy-rewrite**，后端服务全路径 `/api/v1/...`）。

## 红线

仅 dev 占位凭据 / 脱敏 demo（`acme` / `tenant-demo` / `example.com`）；镜像 tag 固定（不用 `latest`）；生产经 ESO/secretRef 注入 `CONTROL_PLANE_DB_PASSWORD` 与 Keycloak admin 口令。
