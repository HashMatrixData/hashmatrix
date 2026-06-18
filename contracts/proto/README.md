# proto · gRPC / 内部 RPC（protobuf）

`<service>/v<major>/*.proto`，包名带 major 版本。从 [`../templates/service.tpl.proto`](../templates/service.tpl.proto) 起。

- 静态门：`buf lint` + `buf breaking`（对基线分支检测破坏，配置见本目录 `buf.yaml`）。
- 破坏性变更走 package `v2`。消费方 `buf generate` 出 stub。
