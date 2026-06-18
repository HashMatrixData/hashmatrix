---
name: local-deploy
description: 在 macOS 本地用 kind + 1ms 镜像缓存跑 hashmatrix 的部署与 E2E。一键拉起/拆除本地集群与持久化镜像缓存（make up/down/warm/status/nuke）、把 charts 部署到本地 kind、排查镜像拉取/代理/磁盘问题。内置设计背景与知识库链接，遇到问题按此回顾。当用户要求本地起集群、本地部署或调试 chart、跑本地 E2E，或镜像拉不动/疑似走了 VPN/缓存异常/Pod ImagePullBackOff 时使用。
argument-hint: "up | warm | status | down | nuke | deploy <chart> | troubleshoot"
---

# local-deploy · 本地测试基础设施操作台（macOS / kind + 1ms 缓存）

把 hashmatrix 的 Helm/E2E 跑在本地 kind 上。**镜像走 1ms 直连不烧 VPN、一次下载跨集群重建复用、一条命令拉起。**
所有资源与脚本在 **`tools/local-infra/`**；**深度原理与逐项排障以 [`tools/local-infra/README.md`](../../../tools/local-infra/README.md) 为准**，本 SKILL 是操作入口与速查。

> 命令统一在 `tools/local-infra/` 下 `make <目标>`。集群名 `hashmatrix-local`，kubectl context `kind-hashmatrix-local`。

## Step 0: 设计背景 + 知识库（遇问题先回顾这里）⭐

**为什么是这套**（详见 README「前因后果」）：
1. 公网镜像走国内 **1ms 镜像站**加速；每个上游 registry 一个子域名。
2. 本机 **MonoCloudProxy(127.0.0.1:8118/8119) 走代理=计费流量** → 镜像拉取**一律绕开代理直连 1ms**。
3. **kind 随时增删 → 镜像不能存集群里** → 集群外跑 5 个持久化 `registry:2` pull-through 缓存（带卷），
   kind 的 containerd 经 `certs.d/<registry>/hosts.toml` 透明指向缓存（命中=0 下载、可离线）。
4. 缓存是优化、**1ms 直连是兜底**：缓存挂了自动落到 `*.1ms.run`，最坏退化为「每次从 1ms 重拉」而非失败。

**registry → 1ms 子域名 → 本地缓存** 映射：

| 上游 registry | 1ms 子域名 | 缓存容器(宿主调试口) |
|---|---|---|
| `docker.io` | `docker.1ms.run` | `cache-docker` (5001) |
| `registry.k8s.io` | `k8s.1ms.run` | `cache-k8s` (5002) |
| `quay.io` | `quay.1ms.run` | `cache-quay` (5003) |
| `ghcr.io` | `ghcr.1ms.run` | `cache-ghcr` (5004) |
| `gcr.io` | `gcr.1ms.run` | `cache-gcr` (5005) |

**知识库 / 参考链接**：
- 1ms 镜像站使用方法：<https://mdoc.cc/mliev/1ms/v1.0.0/16> · 首页 <https://1ms.run/>
- kind 本地 registry：<https://kind.sigs.k8s.io/docs/user/local-registry/>
- containerd `hosts.toml`：<https://github.com/containerd/containerd/blob/main/docs/hosts.md>
- 主仓背景小节：根 `CLAUDE.md` →「🧪 本地测试基础设施」

## Step 1: 一次性前置（每台机器各做一次）

1. **Docker Desktop 代理 Bypass**（否则镜像走 VPN 烧流量）：Settings → Resources → Proxies → Manual →
   "Bypass…" 加入 `.1ms.run`（完整串见 README 附录 A）。→ Apply & Restart。
2. **磁盘上限**：Settings → Resources → Advanced 调大（跑 Kafka/Elastic/Milvus 建议 ≥128G）。
3. 依赖：`docker`(Docker Desktop)、`kind`、`kubectl`、`helm`、`curl`。

> 这些是**宿主级**配置，无法由脚本代劳；异常时先核对这三项。

## Step 2: 拉起底座（幂等，常用）

```bash
cd tools/local-infra && make up        # 5 个缓存 + kind 集群；已存在则秒级复用
make status                            # 看缓存 v2=200/401 与集群清单
```

`make up` 关键动作：①起/复用缓存 ②建集群（**自动剥离宿主 HTTP(S)_PROXY**，避免代理被注入节点）
③把缓存接入 `kind` docker 网 ④等节点 Ready。

## Step 3: 预热镜像到缓存（可选，加速首次部署）

```bash
# 先把 chart 实际依赖的镜像（含 tag）写进 tools/local-infra/images.txt
make warm
```

裸名视为 `docker.io/library/*`；其余须写明 registry 前缀。warm 带 3 次重试容忍冷启动抖动。

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
| 拉镜像慢/疑似走 VPN | 核对 Step 1 的 Bypass 含 `.1ms.run` 且已 Restart；`make status` 看缓存是否 200。 |
| Pod `ImagePullBackOff` | ①`docker network inspect kind` 确认 `cache-*` 已接入 ②该 registry 在 `certs.d/` 是否有同名目录 ③`hosts.toml` 目录名须与 registry **完全一致**。 |
| 缓存是否真的命中 | `_catalog` 对 pull-through 缓存恒空属正常；改看 `docker exec cache-x ls /var/lib/registry/docker/registry/v2/repositories`。 |
| 某上游缓存失效 | containerd 会自动落到 `*.1ms.run` 兜底（退化为每次重拉）；查 `docker logs cache-x`。 |
| 磁盘满 `no space left` | Docker Desktop → Resources → Advanced 调大虚拟磁盘。 |
| 新增上游 registry（如 `docker.elastic.co`） | 新建 `certs.d/<registry>/hosts.toml`；1ms 无对应子域名则改用 `kind load`，或在 `_common.sh` 加缓存项。 |

## 维护本 SKILL / 资源

- 改配置（缓存清单、集群名、端口）：`tools/local-infra/_common.sh`；改后 `make nuke && make up`。
- 改原理/排障文档：`tools/local-infra/README.md`（深度）+ 本 SKILL（速查）+ 根 `CLAUDE.md`（背景小节）三处保持一致。
- **红线**：本 SKILL 与所有 `tools/local-infra/` 文件不得含任何甲方/内网真实参数（内网私服 IP 用占位符）。
