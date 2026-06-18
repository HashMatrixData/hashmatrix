# libs-ts · 公共 TS 依赖（指针 · 已收敛）

> **定位变更（结构决策）**：前端公共 TS 能力**不再在主仓单独发布**，统一收敛进**唯一前端仓 `webui` 的同仓 `packages/*`**（`ui` / `brand` / `theme` / `i18n` / `sdk`），由 webui 的 pnpm monorepo 内部共享。

**为什么收敛**：webui 是当前**唯一的 TS 消费方**且已采用同仓双 app（`apps/console` + `apps/admin`）+ 共享 `packages/*` 结构；再在主仓维护一套对称的 npm 制品（对标 `libs-java`）会引入零复用收益的基建。遵循 YAGNI——**出现第二个 TS 消费方时再把对应 package 提升为主仓发布物**。

- 共享前端组件 / 白标 / 主题 / i18n / API SDK → 见 `services/webui` 的 `packages/*`（spec：`services/webui/docs/00-前端初始化-spec.md`）。
- Java 侧公共依赖仍走主仓 `libs-java`（parent + BOM + `starter-*`，发布 GitHub Packages）。

> 本目录保留为占位/指针，暂不承载源码与制品。
