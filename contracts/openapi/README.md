# openapi · REST 契约（OpenAPI 3.1）

每服务一份 `<service>.yaml`（南北向 + 东西向同步调用）。从 [`../templates/openapi.tpl.yaml`](../templates/openapi.tpl.yaml) 起。

- 静态门：Spectral lint（`../.spectral.yaml`）+ oasdiff 破坏性检测（见 `../CONVENTIONS.md` §测试）。
- 破坏性变更走 `/v2` + 弃用期双跑。消费方从契约生成客户端（openapi-generator），编译期锁字段。
