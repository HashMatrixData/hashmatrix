# ICD · 租户上下文头（X-Tenant-*）

> 跨切面**线契约**：网关（边缘）与所有下游服务之间透传租户上下文的 HTTP 头约定。
> 非代码依赖——产生方（APISIX Lua 插件）与消费方（Java `starter-tenant`）**各自实现、共守此约**。

| 元数据 | 值 |
|--|--|
| id | `icd/tenant-context-headers` |
| status | **draft（待评审）** |
| version | `1.0.0-draft` |
| producer | `services/gateway`（APISIX 插件 `tenant-context.lua`） |
| consumers | 全部 Java 服务（`libs-java` `hashmatrix-starter-tenant`）；任何读租户头的上游 |
| 关联 | 架构 05《多租户与控制平面》§5；主仓 #1（starter-tenant）；gateway#1（X-Tenant 注入） |

## 1. 目的与范围

登录态经 Keycloak 颁发 JWT → 网关校验并解析 org/tenant 声明 → **注入 `X-Tenant-*` 头**下发上游 → 各服务据此做 schema/catalog 路由与行级兜底（数据/计算隔离）。本 ICD 固化这组头的**名称、语义、来源、信任模型与稳定性**，作为产生方与消费方的**单一事实源**。

> 本契约只约束**网关→上游**的可信头。客户端直送的同名头一律不可信（见 §4 信任模型）。

## 2. 头字段定义

| 头 | 语义 | 来源（Keycloak claim） | 必需 | 产生方 | 消费方 |
|--|--|--|--|--|--|
| `X-Tenant-Id` | **稳定租户标识**——数据/计算隔离的路由键（schema/catalog/namespace） | `organization`（回退 `tenant`） | 是 | gateway 注入 | `starter-tenant` → `TenantContext.tenantId` |
| `X-Tenant-Org` | 原始 org 标识/别名（信息性） | 同上 | 否 | gateway 注入 | `starter-tenant` → `TenantContext.org`（可选） |
| `X-Tenant-Subject` | 终端用户主体（`sub`） | `sub` | 否 | gateway 注入 | **预留**（当前 `starter-tenant` 未消费） |

脱敏示例（请求到达上游时）：

```http
X-Tenant-Id: acme
X-Tenant-Org: acme
X-Tenant-Subject: 11111111-1111-4111-8111-111111111111
```

> 多租户模型：`org = 租户`（公网 SaaS=企业 / 私有化=部门），见架构 05 §1。占位一律脱敏（`acme` / `tenant-demo`）。

## 3. 产生方契约（gateway）

`tenant-context.lua` 必须：

1. **入口清洗**：先删除客户端可能携带的 `X-Tenant-*`，再写入网关可信值（防越权伪造）。
2. **消费验签产物**：仅从 `openid-connect` 验签后注入的 `X-Userinfo`（base64 JSON）解析，自身不验签；必须与 `openid-connect` 同路由且在其后执行（priority 2598 紧随 2599）。
3. **fail-closed**：`require_tenant=true`（默认）时——缺 `X-Userinfo`（路由漏配 openid-connect）→ `401`；userinfo 不可解析 → `403`；无租户声明 → `403`。
4. **唯一租户**：一个请求必须解析到**恰好一个**租户；多 org 成员视为歧义 → `403`（不做不确定注入）。
5. 同时把租户暴露为 `$tenant_id` 变量（供 `limit-count` 等按租户限流）——属网关内部用法，**不在本头契约范围**。

## 4. 消费方契约（服务侧）

1. 路由键取 **`X-Tenant-Id`**；`X-Tenant-Org` 仅作信息展示，不得用于隔离路由。
2. `starter-tenant` 默认 `required=false`：**信任边缘已强制**（gateway `require_tenant=true`）。前提是**服务仅在网关之后可达**——见 §5。
3. `X-Tenant-Subject` 为**预留**头；消费方未用时必须**容忍其存在**（tolerant reader），不得因新增头报错。
4. 取不到租户上下文却访问租户隔离资源 → 编程/配置错误（`TenantContextHolder.require()` 抛错），不得静默放行。

## 5. 信任与安全模型

- **网关是租户头的信任根**。`X-Tenant-*` 仅在「经网关、且 `tenant-context` 在 `openid-connect` 之后」注入时可信。
- **服务不得直接对外暴露**：直连（绕过网关）的流量其 `X-Tenant-*` 不可信。私有化/信创部署亦须保证 Java 服务只在 `ns: gateway` 之后可达（NetworkPolicy）。
- 任何新增「读租户头」的消费方默认继承本信任假设；若某服务需独立校验 JWT，应显式声明并走 escape hatch，不在本 ICD 默认范围。

## 6. 语义稳定性（前向兼容关键）

- `X-Tenant-Id` 的**具体形态是部署级固定**（demo 下 `alias==id`；生产可映射 org UUID），但**在单次部署内稳定**。消费方只能依赖「它是稳定路由键」，不得假设其为别名或 UUID 的某一种。
- `X-Tenant-Org` 可能与 `X-Tenant-Id` 取值相同（当前实现）或不同（生产映射后）；消费方不得假设二者相等。

## 7. 版本与兼容策略

- 本 ICD 走 semver。**加法兼容**（新增可选头、放宽来源）允许在 MINOR；**改名 / 改语义 / 收紧必需性**为破坏性，需 MAJOR + 弃用期（产生方双跑新旧头一个窗口）+ 通知全部 consumers。
- 默认 consumer 为 **tolerant reader**：忽略未知头、不依赖头顺序。

## 8. 一致性校验要点（契约测试）

- **头名一致性**：gateway `tenant-context.lua` 的 `id_header`/`org_header`/`subject_header` 默认值，必须等于 `starter-tenant` `TenantProperties` 的 `header`/`orgHeader`（及未来 subject）默认值。当前均为 `X-Tenant-Id` / `X-Tenant-Org` ✅。
- **行为契约**：缺身份 → 边缘 401/403（非放行到上游）；客户端伪造头被清洗。建议产生方侧 e2e（APISIX 起栈 + 伪造头）+ 消费方侧 `TenantContextFilter` 单测共同覆盖。
- 纳入平台契约测试框架后，本 ICD 升 `stable`。
