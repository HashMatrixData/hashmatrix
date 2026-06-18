# CLAUDE.md — hashmatrix（主仓）协作与合规指引

本文件为 Claude Code 及所有协作者在本仓库工作的**强制约束**。违反「信息红线」的内容一律不得提交。

## 🔴 信息红线（强制 · 不可协商）

本仓库为**公开开源仓库**。所有内容（代码、注释、文档、配置样例、提交信息、Issue/PR、分支与标签名）必须满足：

1. **禁止出现任何甲方/客户可识别信息**，包括但不限于：真实单位名称/简称/品牌、人员姓名或账号、招标/合同/立项编号、内部项目代号、甲方专有业务术语、真实数据、具体部署地点、客户网络或系统拓扑。
2. **禁止透漏任何项目机密**：商务/合同条款、里程碑与报价、验收细节、甲方环境参数、真实业务数据样本。
3. **仅允许记录可面向大众公开的内容**：通用技术方案、代码实现、系统架构与产品决策、开源组件选型、通用工程最佳实践。
4. **示例/测试数据一律虚构脱敏**，使用通用占位（如 `example.com`、`acme`、`tenant-demo`），严禁使用任何真实甲方数据。
5. **敏感原始资料一律置于 `.gitignore`、不得入库**（仅本地留存）。

> 判定标准：把本仓任意文件公开到互联网，不会泄露任何客户身份或项目机密。不确定时一律按「不写入」处理。

## 提交前自检（每次 commit / PR 必过）

- [ ] 无甲方名称 / 编号 / 代号 / 人员 / 地点等可识别信息
- [ ] 无商务 / 合同 / 验收 / 报价等项目机密
- [ ] 示例数据均为虚构 / 脱敏
- [ ] 敏感原始资料未入库（已在 `.gitignore`）
- [ ] 提交信息与分支/标签名同样不含上述敏感信息

## 🧭 北极星：产品形态与多租户模式（开发者时刻谨记）

本平台**双模交付**，所有设计与代码都须按此模式思考：

| | 公网 SaaS | 私有化部署 |
|--|--|--|
| 运营 / 品牌 | 我们运营 · **我们公司统一品牌** | 客户环境 · **客户品牌（部署级）** |
| 租户 = | 企业客户 | 客户的部门 |

- **品牌是部署级**（部署期配置注入），**不按租户在运行期动态换肤**。
- **多租户隔离（C 分层桥接）**：控制平面共享 + 数据平面按租户隔离。身份 = Keycloak **Organizations 单 realm**（org=租户，JWT 带 tenant 声明）；数据 = **schema/db-per-tenant**；计算 = **namespace-per-tenant**；由 `control-plane` 编排开通。

**本仓视角（主仓）**：主仓**定义并治理**上述全局模型——多租户控制平面（`services/control-plane`）、公共依赖（`libs-java`，含 `starter-tenant` 租户上下文）、部署（`deploy`，per-tenant Helm release）。任何主仓变更先确认其在双模 / 多租户下成立。

> 全局定义见 `docs/00-主仓初始化-spec.md` 与 `docs/architecture/05-多租户与控制平面.md`。

## 仓库定位

云原生数据中台主仓（super-project）。以 Git Submodule 管理各分系统/服务，负责公共依赖与部署运维（Helm）封装。架构文档见 docs/architecture/ 目录。

技术栈与具体选型**待独立讨论后逐步丰富**，当前为初始脚手架。

## 🧪 本地测试基础设施（macOS / kind + DaoCloud 镜像缓存）

本地跑 Helm / E2E 的统一底座在 **`tools/local-infra/`**，一条命令 `make up`。
**详细原理、用法、逐项排障见 [`tools/local-infra/README.md`](tools/local-infra/README.md)**——
换机器、遇到拉取/代理/磁盘异常时，**先读该 README 的「前因后果」与「附录 B 排障」再动手**。

**为什么是这套（前因后果，便于异常时定位）：**
1. **公网镜像走 [DaoCloud](https://github.com/DaoCloud/public-image-mirror) 加速**，每个上游 registry 一个子域名：
   `docker.io→docker.m.daocloud.io`、`registry.k8s.io→k8s.m.daocloud.io`、`quay.io→quay.m.daocloud.io`、
   `ghcr.io→ghcr.m.daocloud.io`、`gcr.io→gcr.m.daocloud.io`。
2. **本机用 MonoCloudProxy（127.0.0.1:8118/8119），走代理=计费流量**，故**镜像拉取一律绕开代理直连 daocloud**：
   - 缓存容器**不配代理**，访问国内 `*.m.daocloud.io` 本就直连；Docker Desktop 守护进程代理 **Bypass** 加 `.m.daocloud.io`（一次性手动，见 README 附录 A）；
   - `up.sh` 建 kind 时**剥离宿主 `HTTP(S)_PROXY`**——否则 kind 会把 `127.0.0.1:8118` 注入节点
     containerd，而节点内该地址不可达，导致**所有镜像拉取 proxyconnect 失败**（典型坑）。
3. **kind 随时增删，镜像不能存在集群里**：用 **5 个独立于 kind 的 `registry:2` pull-through 缓存**
   （各带持久卷）对应各 `*.m.daocloud.io`；kind 的 containerd 经 `certs.d/<registry>/hosts.toml`
   透明指向缓存（命中=0 下载、可离线）→ **一次下载，反复重建集群复用**。
4. **缓存是优化、daocloud 直连是兜底**：某缓存挂了自动落到 `*.m.daocloud.io`，最坏退化为「每次重拉」而非失败。

> ⭐ **为什么是 DaoCloud 而非 1ms**：缓存容器故意不配代理（不烧 VPN）。DaoCloud 对**所有** registry 的 blob
> 都**自己中转(200)**，无代理缓存能落盘；而 1ms 对 k8s/quay/gcr/ghcr 的 blob 是 **307 重定向回国外原站 CDN**，
> 无代理缓存够不到 → `404 blob unknown`（早期 `make warm` 7/8 失败的根因，付费也不改）。**换上游前先确认它自己中转 blob。**

> 不同机器的代理/网络/磁盘配置不同，配置项（缓存清单、集群名、端口、Bypass 列表、上游）集中在
> `tools/local-infra/_common.sh` 与 README，按需调整；调整后 `make nuke && make up`。
