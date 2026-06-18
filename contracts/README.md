# contracts · 接口契约（ICD）

各子模块间的接口契约统一在此维护，对应交付物中的「接口控制文件（ICD）」。

- `openapi/` —— REST 接口（OpenAPI 3）
- `proto/` —— gRPC / 内部 RPC（protobuf）
- 约定：先改契约、再改实现；契约变更需评审。

## ICD 草案

- [`tenant-context-headers-icd.md`](./tenant-context-headers-icd.md) —— 租户上下文头 **X-Tenant-***（gateway 注入 ↔ `starter-tenant` 消费，跨切面线契约，草案）。
- [`governance-metadata-icd.md`](./governance-metadata-icd.md) —— 数据治理元数据**供数 API + 变更事件**（草案，待评审）。

> 当前为占位 + 草案，具体接口随各子模块设计逐步补充。
> 契约目录结构 / 文档规范 / 版本兼容与契约测试架构：治理框架另行规范（讨论中）。
