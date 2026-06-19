---
name: create-milestone
description: 在主仓发起跨子仓的开发里程碑——统一命名、各子仓建同名 milestone 协同、每仓一个「可自拆解简报」tracking issue。内置跨切面调研维度、不可返工决策的落档与确认、master 总纲模板、自拆解简报模板、gh 操作手册与实测工程坑。当用户要求创建/发起/规划一个开发里程碑、跨仓 Milestone、或把里程碑拆分到各子仓时使用。
argument-hint: "[里程碑主题/目标] | 调研 | 建 milestone | 简报模板 | 验证"
---

# create-milestone · 主仓发起·子仓协同的里程碑工作流

在**主仓发起**一个开发里程碑：写一份跨仓总纲，在主仓与各子仓建**同名 milestone**，每个仓挂一个**「可自拆解简报」**tracking issue。本 skill = 操作手册 + 模板 + 实测坑。

> **心智模型（四条铁律）**：
> 1. **主仓发起、子仓协同**：里程碑总在主仓 `docs/milestones/` 落总纲；各子仓建**同名** milestone 承接，子仓 milestone **不脱离主仓总纲单独存在**。
> 2. **Milestone = 串联与验收容器，不解决具体问题**：可以粗粒度，但每仓 tracking issue 必须 **self-sufficient**——让里程碑服务于"拆分 + 验收"。
> 3. **骨架不返工、架构不错误**：先确认**不可返工的架构决策**，再建里程碑。
> 4. **公开仓红线**：milestone / issue / 分支 / 标签名同样禁含甲方可识别信息（见 `CLAUDE.md`）。
>
> **事实源**：架构 `docs/architecture/`、PRD `prototype/docs/`、部署 `deploy/`、契约 `contracts/`、公共依赖 `libs-java/`、本地底座 `tools/local-infra/`。
> **已落地范例**：`docs/milestones/M1-单命名空间端到端贯通.md`（本流程的首个产出，可作模板参照）。

---

## A. 一次里程碑创建的产物清单

1. **主仓 master 总纲**：`docs/milestones/<M?-主题>.md`（单一事实源，各仓 issue 回链它）。
2. **跨切面基线入 `CLAUDE.md`**（凡需"不重复手维护"的全局约定，如端口分配、命名规范）。
3. **同名 milestone**：主仓 + 每个相关子仓各一个。
4. **每仓一个 tracking issue =「可自拆解简报」**，挂到该仓 milestone。
5. **（按需）决策落档**：把不可返工决策写回 `docs/architecture/`、相关 PRD、或修订 `contracts/`。

---

## B. 流程（6 步）

1. **调研现状**（§C 维度，跨仓盘点；可用 subagent 并行，要求回**精确路径** + 真/stub/缺失）。
2. **决策**：列出**不可返工的架构决策**，分两类——
   - *我拍板*（有安全默认、可后置/预留的范围取舍）：直接定，写进总纲。
   - *必须问用户*（真正阻塞、且用户点名要确认的，如身份/Account 模型、白标隔离粒度、品牌配色）：用 **AskUserQuestion** 只问这几个，每个带推荐项。**don't pause to ask** 那些有明显默认的。
3. **写 master 总纲**（§D 模板）。
4. **跨切面基线入 `CLAUDE.md`**（如端口表 + 指向总纲）。
5. **建 milestone + tracking issue**（§F 命令；issue body 用 §E 简报模板，逐仓 grounded 到真实代码）。
6. **验证**：每仓 milestone `open_issues=1`、issue title 统一、body 非空（§F 末）。

> 调研→决策之间若发现**契约/代码与新决策冲突**（例：某 ICD 写死的约束与新身份模型矛盾），在总纲里**显式列为"必须同步修订项"**，并落进对应子仓的简报，否则代码与契约会打架。

---

## C. 调研维度清单（每次里程碑必查的跨切面）

对每个维度产出：**落实矩阵**（仓 × 状态 × 证据路径）+ **缺口** + **各仓本里程碑建议交付项**。维度间多可并行（一维/一仓一个 subagent）。

| 维度 | 查什么 | 典型坑 |
|--|--|--|
| **契约** | `contracts/` 标准是否定义；各子仓是否落实 + **CLAUDE.md 是否有契约块**；producer/consumer 映射；是否 **vendoring 漂移**（子仓存了主仓契约副本并改动） | 标准在、实现侧零消费；子仓 CLAUDE.md 缺块；vendored 副本漂移 |
| **Helm / 部署** | `deploy/charts` 哪些**真实** vs **仅 NOTES.txt 占位**；契约是"**子仓交付 image、主仓 owns charts**"还是反之；单 ns demo 起 Pod 的阻塞 | 占位子 chart `image.repository=""`；空壳子仓产不出 image |
| **端口 / 本地调试** | 各服务 `server.port` / 中间件端口**冲突**（多服务默认 8080、多 PG 抢 5432…）；能否单机并行 | 全默认 8080；建议落 `CLAUDE.md` + `Makefile` 用 `${ENV:默认}` |
| **身份 / RBAC / 租户** | Keycloak Organizations、JWT tenant 透传、control-plane 开通(真实/stub)、libs `starter-tenant`/`starter-security`；**User/Account/Membership 数据模型**（最易返工） | 网关契约写死的约束与"多租户/切换"诉求冲突；API 裸奔无鉴权 |
| **前端白标** | 品牌**部署级注入**隔离粒度（不按租户换肤）；默认配色/Logo；导航是否对齐 PRD 模块 | 默认色与原型不一致；导航还是 demo 项 |
| **各仓 scaffold-vs-real** | 栈/版本、真实代码 vs 脚手架 vs 空壳、入口级功能(probe/controller)、Dockerfile/健康端点/端口 | 个别子仓是空壳（无 pom/src），拖累 umbrella |

---

## D. master 总纲模板（`docs/milestones/<M?-主题>.md`）

```
# M? · <主题>（统一里程碑）
> 范围：跨 N 仓的第 ? 个里程碑协调总纲；各仓以同名 Milestone 承接。
> 定位：可向上阶段性演示的<骨架/能力>——不保证完整可用，但基础骨架不可返工、架构不可错误。
> 红线：本文与各仓 Issue/分支/提交均为公开内容（见各仓 CLAUDE.md）。

## 1. 目标与验收
统一目标 / 演示验收(Demo Acceptance，可勾选可验证) / 非目标(明确后置，防镀金)

## 2. 已确认的关键决策（不可返工）
| # | 决策 | 结论 | 影响面 |   ← D1/D2…，每条写清结论与影响；含"必须同步修订"项

## 3. 跨切面基线（如统一端口分配）
约定 + 表（应用端口/管理端口/中间件/前端 dev），与 CLAUDE.md 一致

## 4. 各仓 M? 交付清单（同名 Milestone 承接）
每仓一小节：粗工作项勾选（详细 grounded 简报在各仓 tracking issue）

## 5. 跨仓依赖与推进顺序
① 地基 → ② … → ⑤ 串联演示（ASCII 流）

## 6. 风险与待澄清（迭代中持续澄清）
R1/R2… + 不阻塞骨架的待澄清项（数据模型已按可扩展预留）
```

---

## E. tracking issue =「可自拆解简报」模板（核心标准）⭐

每个子仓的 tracking issue 套这个骨架。**判定门槛：新入职工程师只读本简报 + 本仓代码，就能自拆子任务并自验收、不必再问人。**

```
# M? 自拆解简报 · <repo>
> 粗粒度里程碑简报。只读本简报 + 本仓代码即可自拆子任务并自验收，无需问人。
> 上位总纲：主仓 docs/milestones/<M?-主题>.md

## 0. 一句话目标
## 1. 起点 · 代码现状        ← 精确到路径/类/配置，每项标 [真实]/[stub]/[缺失]；列本地起法
## 2. 范围边界               ← ✅ M? 做 / ⛔ 明确不做(后置)
## 3. 工作项 & 拆分指引       ← 每个粗工作项 WPx 必含：
                              要达成 · 入手点(改哪里/路径) · 参考(照着做的契约/现有模式/文档) ·
                              可拆为子任务(示例 - [ ] …) · 验收(可验证)
## 4. 关键参考               ← 契约·架构·PRD·现有代码模式，全给精确路径
## 5. 依赖 & 约束            ← 端口(app/mgmt) · 跨仓依赖(需谁/被谁依赖) · 受哪条决策 D? 约束
## 6. 完成判据               ← demo 级勾选 + 红线自检 + readiness/health 绿
```

**就绪四要件（缺一即"新人会来问人"）**：① 起点·代码现状（精确路径 + 真/stub/缺失）② 拆分指引（每工作项给入手点/参考/可拆子任务）③ 关键参考（精确链接）④ 验收判据。

---

## F. gh 操作手册

```bash
ORG=<github-org>
# 0) 列出相关仓（排除非本里程碑的仓，如插件/工具仓）
gh repo list "$ORG" --limit 50 | awk '{print $1}'
# 1) 查现有 milestone 避免重复
gh api "repos/$ORG/<repo>/milestones" --jq '.[]|"\(.number)\t\(.title)"'
# 2) 建 milestone（统一 TITLE 跨仓；无 due date 就不传）
num=$(gh api "repos/$ORG/<repo>/milestones" -f title="$TITLE" -f description="$DESC" -f state=open --jq .number)
# 3) 建 tracking issue 并挂到 milestone（--milestone 接标题或编号）
gh issue create --repo "$ORG/<repo>" --title "M? 自拆解简报 · 入口级交付" \
  --body-file "/tmp/brief_<repo>.md" --milestone "$TITLE"
# 4) 升级/改写已存在的 issue
gh issue edit <num> --repo "$ORG/<repo>" --title "$TITLE" --body-file "/tmp/brief_<repo>.md"
# 5) 验证（每仓应 open_issues=1）
gh api "repos/$ORG/<repo>/milestones/<n>" --jq '"\(.title) open_issues=\(.open_issues)"'
```

要点：**统一 TITLE 跨所有仓**；body 一律走 `--body-file`（避免引号地狱）；批量循环用 `while IFS='|' read -r ...` 安全分割。

---

## G. 工程坑（实测，按此规避）

- **红线先行**：push / 建 issue 前必跑红线 grep；命中**具体可识别信息**才停核查（策略说明里的"甲方/红线"通用词可保留）。
  ```bash
  grep -rniE "星网|甲方真实名|招标|合同编号|立项|真实IP|内部代号|[0-9]{11}" <改动范围>
  ```
- **macOS 默认是 zsh + bash 3.2**：① bash 3.2 **无 `declare -A`** 关联数组 → 用 `case` 或写 `*.sh` 文件 `bash script.sh` 跑；② **zsh 不对未引用变量做词分割** → 循环别用 `set -- $var`，用 `while IFS='|' read -r a b c` 或显式引号（`gh` 一行验证脚本最容易栽在这）。
- **push 撞并发**：远端 `main` 可能在你本地 ref 之后前进（别的 PR）。被拒时 `git fetch` 看分叉，改动不相交就 **`git rebase origin/main` 后再 push，绝不 `--force`**。
- **子模块次序**：子仓改动**先在子仓 `commit + push`**，再回主仓 `git add <子模块路径>` 记录 gitlink；rebase 后用 `git submodule update` 同步工作树（避免 detached/gitlink 漂移）。
- **不臆造 due date**：GitHub milestone 可留空；除非用户给了明确日期/可引用的工期条件，否则不设。
- **决策要确认的就确认**：身份/Account 模型、白标隔离粒度、品牌配色这类**不可返工 + 用户点名**的，用 AskUserQuestion 问；有安全默认的范围取舍直接拍板并在总纲留痕。

---

## 反模式（避免）

- ❌ **在子仓单独发起 milestone** 而无主仓总纲串联 —— 违"主仓发起"，协同会散。
- ❌ **tracking issue 只堆 checklist**（无起点/参考/验收）—— 新人必来问人，违里程碑"服务于拆分与验收"的本义。
- ❌ **把不可返工的架构决策一刀切**（身份模型 / 隔离粒度 / charts 归属 / 端口契约）不与用户确认 —— 返工代价最高。
- ❌ **在主仓总纲里堆子仓的具体实现细节** —— 具体落 grounded 简报到各子仓 issue；总纲只串联与验收。
- ❌ **臆造 due date / 写入报价或验收细节** —— 红线：商务/验收细节不入库。
- ❌ **force-push 解决并发**、或子模块指针不同步就提交。
- ❌ **milestone / issue / 分支 / 标签名含甲方可识别信息** —— 同正文一样守红线。
