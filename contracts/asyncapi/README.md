# asyncapi · 异步事件契约（AsyncAPI 2.6 + JSON Schema）

每领域一份 `<domain>.yaml`（Kafka 领域事件 / 变更通知）。从 [`../templates/asyncapi.tpl.yaml`](../templates/asyncapi.tpl.yaml) 起。

- envelope 必带 `schemaVersion`；消费方 tolerant reader。
- 静态门：Spectral lint。加字段=向后兼容（MINOR）；删/改字段=破坏（MAJOR + 双跑）。
