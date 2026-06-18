---
name: local-deploy
description: 在 macOS 本地用 kind + DaoCloud 镜像缓存跑 hashmatrix 的部署与 E2E。一键拉起/拆除本地集群与持久化镜像缓存（make up/down/warm/status/nuke）、把 charts 部署到本地 kind、排查镜像拉取/代理/磁盘问题。内置设计背景与知识库链接，遇到问题按此回顾。当用户要求本地起集群、本地部署或调试 chart、跑本地 E2E，或镜像拉不动/疑似走了 VPN/缓存异常/Pod ImagePullBackOff 时使用。
argument-hint: "up | warm | fetch IMG=... | status | down | nuke | deploy <chart> | troubleshoot"
---

# local-deploy · 本地测试基础设施操作台（macOS / kind + DaoCloud 缓存）

把 hashmatrix 的 Helm/E2E 跑在本地 kind 上。**镜像走 DaoCloud 直连不烧 VPN、一次下载跨集群重建复用、一条命令拉起。**
所有资源与脚本在 **`tools/local-infra/`**；**深度原理与逐项排障以 [`tools/local-infra/README.md`](../../../tools/local-infra/README.md) 为准**，本 SKILL 是操作入口与速查。

> 命令统一在 `tools/local-infra/` 下 `make <目标>`。集群名 `hashmatrix-local`，kubectl context `kind-hashmatrix-local`。

## Step 0: 设计背景 + 知识库（遇问题先回顾这里）⭐

**为什么是这套**（详见 README「前因后果」）：
1. 公网镜像走国内 **DaoCloud 镜像站**加速；每个上游 registry 一个子域名。
2. 本机 **MonoCloudProxy(127.0.0.1:8118/8119) 走代理=计费流量** → 镜像拉取**一律绕开代理直连 daocloud**。
3. **kind 随时增删 → 镜像不能存集群里** → 集群外跑 5 个持久化 `registry:2` pull-through 缓存（带卷），
   kind 的 containerd 经 `certs.d/<registry>/hosts.toml` 透明指向缓存（命中=0 下载、可离线）。
4. 缓存是优化、**daocloud 直连是兜底**：缓存挂了自动落到 `*.m.daocloud.io`，最坏退化为「每次重拉」而非失败。

> ⭐ **为什么是 DaoCloud 而不是 1ms（关键，换源前必读）**：我们的缓存容器**故意不配代理**（为了不烧 VPN）。
> DaoCloud 对**所有** registry 的 blob 都**自己中转(HTTP 200)**，无代理缓存能直接落盘。
> 而 1ms 对 k8s/quay/gcr/ghcr 的 blob 是 **307 重定向回国外原站 CDN**，无代理缓存够不到 → `404 blob unknown`
> （这正是早期 `make warm` 7/8 失败的根因，付费也不改变 307）。**换任何新上游前，先确认它自己中转 blob。**

**registry → DaoCloud 子域名 → 本地缓存** 映射：

| 上游 registry | DaoCloud 子域名 | 缓存容器(宿主调试口) |
|---|---|---|
| `docker.io` | `docker.m.daocloud.io` | `cache-docker` (5001) |
| `registry.k8s.io` | `k8s.m.daocloud.io` | `cache-k8s` (5002) |
| `quay.io` | `quay.m.daocloud.io` | `cache-quay` (5003) |
| `ghcr.io` | `ghcr.m.daocloud.io` | `cache-ghcr` (5004) |
| `gcr.io` | `gcr.m.daocloud.io` | `cache-gcr` (5005) |

**知识库 / 参考链接**：
- DaoCloud 公共镜像站（开源、免费、免注册）：<https://github.com/DaoCloud/public-image-mirror>
- kind 本地 registry：<https://kind.sigs.k8s.io/docs/user/local-registry/>
- containerd `hosts.toml`：<https://github.com/containerd/containerd/blob/main/docs/hosts.md>
- 主仓背景小节：根 `CLAUDE.md` →「🧪 本地测试基础设施」

## Step 1: 一次性前置（每台机器各做一次）

1. **Docker Desktop 代理 Bypass**（保障宿主侧直连 daocloud 不绕 VPN）：Settings → Resources → Proxies → Manual →
   "Bypass…" 加入 `.m.daocloud.io`（完整串见 README 附录 A）。→ Apply & Restart。
2. **磁盘上限**：Settings → Resources → Advanced 调大（跑 Kafka/Elastic/Milvus 建议 ≥128G）。
3. 依赖：`docker`(Docker Desktop)、`kind`、`kubectl`、`helm`、`curl`。

> 这些是**宿主级**配置，无法由脚本代劳；异常时先核对这三项。

## Step 2: 拉起底座（幂等，常用）

```bash
cd tools/local-infra && make up        # 5 个缓存 + kind 集群；已存在则秒级复用
make status                            # 看缓存 v2=200 与集群清单
```

`make up` 关键动作：①起/复用缓存 ②建集群（**自动剥离宿主 HTTP(S)_PROXY**，避免代理被注入节点）
③把缓存接入 `kind` docker 网 ④等节点 Ready。

## Step 3: 预热镜像到缓存（可选，加速首次部署）

```bash
# 先把 chart 实际依赖的镜像（含 tag）写进 tools/local-infra/images.txt
make warm
```

裸名视为 `docker.io/library/*`；其余须写明 registry 前缀。warm 幂等、带重试，失败行会打印**上游真实报错**。

## Step 4: 部署 chart 到本地 kind

- **context 必须是 `kind-hashmatrix-local`**：`kubectl config use-context kind-hashmatrix-local`。
- 部署走 **`helm-deploy` skill**（渲染门 + 红线守卫 + diff/apply）；镜像由缓存透明解析，**无需改 image 名**。

```bash
helm upgrade --install <svc> deploy/charts/<svc> -n <ns> --create-namespace
```

- **自研镜像**（`ghcr.io/hashmatrixdata/*`）本地构建后用 `kind load`，不走 registry：
  ```bash
  kind load docker-image ghcr.io/hashmatrixdata/<svc>:dev --name hashmatrix-local
  ```
  私有镜像也走这条路（绕开 registry 鉴权）。

## Step 5: E2E 验证标准

1. `make up` 已就绪（幂等）。
2. `helm upgrade --install …` 部署被测服务。
3. `kubectl --context kind-hashmatrix-local get pod -A`，确认无 `ImagePullBackOff`、被测 Pod `Running`。
4. **数据红线**：E2E 数据一律虚构脱敏（`example.com`/`acme`/`tenant-demo`），禁止真实甲方数据。
5. 部署前过 `helm-deploy` 的 `lint/template/kubeconform` 渲染门。

## Step 6: 拆除

```bash
make down     # 删集群，保留缓存 → 下次 up 不重新下载（首选）
make nuke     # 彻底重置：删集群 + 清空缓存卷（会重新下载）
```

## Step 7: 排障速查（先读 README 附录 B 完整表）

| 症状 | 处置 |
|---|---|
| Pod `proxyconnect … 127.0.0.1:8118 … refused` | kind 把宿主代理注入了节点。`up.sh` 已自动剥离；若手动建集群，`env -u HTTP_PROXY -u HTTPS_PROXY … kind create`。核对：`docker exec <node> grep -ri proxy /etc/systemd/system/containerd.service.d/` 应为空。 |
| 拉镜像慢/疑似走 VPN | 核对 Step 1 的 Bypass 含 `.m.daocloud.io` 且已 Restart；`make status` 看缓存是否 200。DaoCloud 免费站偶有限速，warm 可重跑。 |
| Pod `ImagePullBackOff` | ①`docker network inspect kind` 确认 `cache-*` 已接入 ②该 registry 在 `certs.d/` 是否有同名目录 ③`hosts.toml` 目录名须与 registry **完全一致**。 |
| 缓存是否真的命中 | `_catalog` 对 pull-through 缓存恒空属正常；改看 `docker exec cache-x ls /var/lib/registry/docker/registry/v2/repositories`。 |
| 拉取 `unknown blob`/`blob unknown`（404） | 上游不自己中转该 blob。daocloud 偶缺 → `make fetch IMG="<完整镜像>"`。**若你把上游换成了 1ms 这类"重定向回原站"的镜像站，k8s/quay 必 404 → 换回自己中转 blob 的上游**（见 Step 0 ⭐）。 |
| `make warm` 大面积失败/极慢 | DaoCloud 免费站 blob 限速/抖动；warm 幂等，**重跑只补未成功项**（失败行打印上游报错）；持续失败用 `make fetch`。 |
| docker 命令反复弹「访问其他 App 数据」 | IntelliJ 终端下 `credsStore` 读 Docker Desktop 数据触发 TCC；脚本已统一空 `DOCKER_CONFIG` 规避，手动 pull 时加 `DOCKER_CONFIG=$TMPDIR/hm-local-infra-docker`。 |
| 磁盘满 `no space left` | Docker Desktop → Resources → Advanced 调大虚拟磁盘。 |
| 新增上游 registry（如 `docker.elastic.co`） | 新建 `certs.d/<registry>/hosts.toml` + 在 `_common.sh` 的 `CACHES` 加一项指向 daocloud 子域名（`elastic.m.daocloud.io`）。**确认新上游自己中转 blob**。 |

## 维护本 SKILL / 资源

- 改配置（缓存清单、集群名、端口、上游）：`tools/local-infra/_common.sh`；改后 `make nuke && make up`。
- 改原理/排障文档：`tools/local-infra/README.md`（深度）+ 本 SKILL（速查）+ 根 `CLAUDE.md`（背景小节）三处保持一致。
- **红线**：本 SKILL 与所有 `tools/local-infra/` 文件不得含任何甲方/内网真实参数（内网私服 IP 用占位符；账户凭据放 gitignored `_secret.env`）。
