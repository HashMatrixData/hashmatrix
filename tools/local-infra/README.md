# 本地测试基础设施（macOS / kind + 1ms 镜像缓存）

> 面向 **hashmatrix 全部子项目** 的本地 K8s/E2E 标准底座。一次性建好，反复复用。
> 设计目标：**镜像走 1ms 不烧 VPN 流量 · 一次下载跨集群重建复用 · 一条命令拉起。**

---

## 0. 前因后果（为什么是现在这套）

不同机器的网络/代理环境不同，本节解释**每个决定背后的约束**，便于在异常时定位与调整。

1. **公网镜像在国内直连慢/不通** → 用国内镜像站 [1ms.run](https://1ms.run/) 加速。
   1ms 对**每个上游 registry 提供独立子域名**（实测均 `https` 直连可达，返回 401 = 正常的 registry 鉴权响应）：

   | 上游 registry | 1ms 子域名 |
   |---|---|
   | `docker.io` | `docker.1ms.run` |
   | `registry.k8s.io` | `k8s.1ms.run` |
   | `quay.io` | `quay.1ms.run` |
   | `ghcr.io` | `ghcr.1ms.run` |
   | `gcr.io` | `gcr.1ms.run` |
   | `k8s.gcr.io`(已弃用) | `k8s-gcr.1ms.run` |

   > 用法详见 1ms 官方文档：<https://mdoc.cc/mliev/1ms/v1.0.0/16>（镜像站使用方法）。

2. **Docker 的 `registry-mirror` 只对 docker.io 生效** → 对 k8s/quay/ghcr/gcr 无效。
   所以我们**不**靠 daemon 的 `registry-mirrors`，而是在 **kind 的 containerd** 里用 `certs.d/<registry>/hosts.toml`
   对每个上游单独做镜像重定向（见 `certs.d/`）。这样**无需改任何 chart 里的 image 名**。

3. **本机用 MonoCloudProxy（127.0.0.1:8118/8119）做代理，走代理的流量是计费的** →
   绝不能让镜像拉取走代理。两处保证：
   - **Docker Desktop 守护进程代理**：Settings → Resources → Proxies → Manual，
     **Bypass** 列表已加入 `.1ms.run`（及 `localhost,127.0.0.1,::1,.local,host.docker.internal,内网段`）。
     于是宿主 `docker pull` / `kind create`（拉 `kindest/node`）连 `*.1ms.run` 时**直连、绕开 VPN**。
   - **容器出网代理**：Docker Desktop 的「Containers」代理本机为空 →
     缓存容器（registry:2）访问 `*.1ms.run` 本就**不走代理、直连**。

4. **kind 随时 create/delete，节点 containerd 里的镜像会随集群删除而丢失** →
   不能把镜像存在集群里。改为在**集群之外**跑 5 个持久化 `registry:2` **pull-through 缓存**
   （各带 docker named volume），每个对应一个上游 `*.1ms.run`。
   首次拉取从 1ms 落盘到缓存卷；之后**无限次重建 kind 都命中本地缓存（0 下载、可离线）**。

5. **多上游导致单一缓存会有路径冲突** → 每个上游一个独立缓存容器（命名空间天然隔离），
   而不是一个大缓存。

### 数据流（最终形态）

```
Pod 拉 registry.k8s.io/ingress-nginx/controller:vX
  └─ kind containerd 读 certs.d/registry.k8s.io/hosts.toml
       1) http://cache-k8s:5000   ← 命中缓存卷：0 下载、可离线 ✅
       2) https://k8s.1ms.run     ← 未命中：1ms 直连落盘（不走 VPN）
            （宿主守护进程/容器均已 bypass MonoProxy）
```

> **健壮性**：缓存是「优化」，1ms 直连是「兜底」。某个缓存容器挂了，containerd 自动落到第 2 个 host（`*.1ms.run`），
> 最坏退化为「每次重建从 1ms 重拉」，而不是部署失败。

---

## 1. 快速开始

```bash
cd tools/local-infra

make up        # 拉起 5 个缓存 + kind 集群（幂等，常用）
make warm      # 可选：按 images.txt 预热常用镜像到缓存
make status    # 查看缓存/集群状态
make down      # 删集群、保留缓存（镜像不丢）→ 下次 make up 秒级复用
make nuke      # 彻底重置（连缓存卷一起清，会重新下载）
```

前置依赖：`docker`(Docker Desktop)、`kind`、`kubectl`、`helm`、`curl`。
**前置一次性手动配置**见下方「附录 A：Docker Desktop 代理旁路」。

---

## 2. 目录结构

```
tools/local-infra/
├── README.md            ← 本文（详细原理 + 排障）
├── Makefile             ← make up/down/warm/status/nuke
├── _common.sh           ← 公共变量/函数（缓存清单在此）
├── registries.sh        ← 管理 5 个 pull-through 缓存
├── up.sh / down.sh      ← 集群拉起 / 拆除（down 保留缓存）
├── warm.sh + images.txt ← 预热镜像清单
├── kind-cluster.yaml    ← kind 配置（containerd certs.d 重定向）
└── certs.d/             ← 每个上游一份 hosts.toml（缓存优先 → 1ms 兜底）
    ├── docker.io/ registry.k8s.io/ quay.io/ ghcr.io/ gcr.io/
```

---

## 3. 本地 E2E 执行标准（所有子项目共用）

1. **底座**：`cd tools/local-infra && make up`（幂等；缓存已存在则秒级复用）。
2. **部署**：各项目用 `helm upgrade --install <svc> deploy/charts/<svc> -n <ns> --create-namespace`；
   镜像由缓存透明解析，**无需改 image 名**。
3. **自研镜像**：本地 `docker build` 后 `kind load docker-image ghcr.io/hashmatrixdata/<svc>:dev --name hashmatrix-local`
   （走本地、不触网；私有镜像也走这条路，绕开 registry 鉴权）。
4. **数据红线**：E2E 数据一律虚构脱敏（`example.com` / `acme` / `tenant-demo`），
   禁止任何真实甲方数据（见根 `CLAUDE.md` 信息红线）。
5. **静态门**：部署前先过 `helm-deploy` skill 的 `lint / template / kubeconform` 渲染门。
6. **拆除**：`make down` 只删集群、**留缓存**；`make nuke` 才彻底清。

---

## 附录 A：Docker Desktop 代理旁路（一次性手动）

Docker Desktop → **Settings → Resources → Proxies → Manual proxy configuration**，
在 **"Bypass proxy settings for these hosts & domains"** 填入：

```
.1ms.run,localhost,127.0.0.1,::1,.local,host.docker.internal,<内网私服IP或域名>,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16
```

→ **Apply & Restart**。含义：让守护进程拉 `*.1ms.run`、内网仓库、本地地址时**直连**，其余仍可经 8118 兜底。
`<内网私服IP或域名>` 按本机实际内网镜像仓填写（如自建 registry、CNB `docker.cnb.cool` 等）；无则删除该项。

> ⚠️ 不要直接手改 `~/Library/Group Containers/group.com.docker/settings-store.json`——
> Docker Desktop 退出时会覆写。务必走 UI。

---

## 附录 B：排障（换机器 / 异常时按此分析）

| 症状 | 排查 |
|---|---|
| 拉镜像很慢/像在走代理 | 确认附录 A 的 bypass 含 `.1ms.run` 且已 **Restart**；`make status` 看各缓存 `v2=200/401`。 |
| `make status` 某缓存 `v2=000` | 该缓存容器没起来：`docker logs cache-xxx`；或上游 1ms 子域名变更，核对 `_common.sh` 的 `CACHES`。 |
| Pod 报 `proxyconnect tcp: dial tcp 127.0.0.1:8118: connect: connection refused` | kind 把宿主 `HTTP(S)_PROXY` 注入了节点 containerd，而节点内 `127.0.0.1` 不是宿主、代理不可达。`up.sh` 已在 `kind create` 前 `env -u HTTP_PROXY ...` 剥离代理；若你手动建集群或改了 `up.sh`，务必同样剥离。验证：`docker exec <node> cat /etc/systemd/system/containerd.service.d/*.conf \| grep -i proxy` 应为空。 |
| Pod 一直 `ImagePullBackOff` | 1) `docker network inspect kind` 确认 `cache-*` 已接入（`make up` 会自动 connect）；2) 该镜像所在 registry 是否在 `certs.d/` 有对应目录；3) 看 `certs.d/<registry>/hosts.toml` 目录名是否与镜像 registry **完全一致**；4) `_catalog` 对 pull-through 缓存恒为空属正常，验证缓存命中改看 `docker exec cache-x ls /var/lib/registry/docker/registry/v2/repositories`。 |
| 新增了一个上游 registry（如 `docker.elastic.co`） | 新建 `certs.d/<registry>/hosts.toml`；若 1ms 无对应子域名，则该 registry 直接走 1ms 兜底不可用——改用 `kind load`，或在 `_common.sh` 加一个指向可用上游的缓存。 |
| 磁盘报满 / `no space left` | Docker Desktop **Settings → Resources → Advanced** 调大 *Virtual disk limit*（默认偏小，本机磁盘充裕可设 200G+）。 |
| 私有镜像（如私有 ghcr）拉不动 | pull-through 缓存默认匿名；改用 `kind load`，或给 `cache-ghcr` 加 `REGISTRY_PROXY_USERNAME/PASSWORD` 环境变量后 `make nuke && make up`。 |
| `daemon.json` 里残留 `https://hub.docker.com/` | 它不是合法 mirror 端点，可在 **Docker Engine** 设置页删除，仅保留 `https://docker.1ms.run`（可选优化）。 |
| 想换集群名/端口 | `_common.sh` 改 `CLUSTER`；缓存宿主端口在 `CACHES` 列。改后 `make nuke && make up`。 |

参考：[kind 官方 registry 配置](https://kind.sigs.k8s.io/docs/user/local-registry/) ·
[containerd hosts.toml](https://github.com/containerd/containerd/blob/main/docs/hosts.md) ·
[1ms 镜像站文档](https://mdoc.cc/mliev/1ms/v1.0.0/16)
