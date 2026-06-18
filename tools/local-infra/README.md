# 本地测试基础设施（macOS / kind + DaoCloud 镜像缓存）

> 面向 **hashmatrix 全部子项目** 的本地 K8s/E2E 标准底座。一次性建好，反复复用。
> 设计目标：**镜像走 DaoCloud 不烧 VPN 流量 · 一次下载跨集群重建复用 · 一条命令拉起。**

---

## 0. 前因后果（为什么是现在这套）

不同机器的网络/代理环境不同，本节解释**每个决定背后的约束**，便于在异常时定位与调整。

1. **公网镜像在国内直连慢/不通** → 用国内镜像站加速。每个上游 registry 一个独立子域名：

   | 上游 registry | DaoCloud 子域名 |
   |---|---|
   | `docker.io` | `docker.m.daocloud.io` |
   | `registry.k8s.io` | `k8s.m.daocloud.io` |
   | `quay.io` | `quay.m.daocloud.io` |
   | `ghcr.io` | `ghcr.m.daocloud.io` |
   | `gcr.io` | `gcr.m.daocloud.io` |
   | `mcr.microsoft.com` | `mcr.m.daocloud.io` |
   | `nvcr.io` | `nvcr.m.daocloud.io` |

   > DaoCloud 公共镜像站开源、免费、无需注册/token：<https://github.com/DaoCloud/public-image-mirror>

   > ⭐ **为什么是 DaoCloud 而不是 1ms（血泪教训，换源前务必读）**：
   > 镜像 = **manifest（小清单）+ blob（几十~几百 MB 的层）**。关键差异在「谁来送 blob」：
   > - **DaoCloud**：对**所有** registry 的 blob 都**自己中转**（HTTP `200`）→ 你从国内 daocloud 直接下，**不碰国外原站**。
   > - **1ms（mliev）**：只有 `docker.io` 的 blob 自己托管；对 `quay.io`/`registry.k8s.io`/`gcr`/`ghcr`
   >   的 blob 返回 **`307` 重定向回国外原站 CDN**（如 `cdn01.quay.io`）让你自己去下。
   >
   > 本设施的缓存容器**故意不配代理**（见第 3 点，为的是不烧 VPN）。配 1ms 时，遇到 307 跳国外 CDN，
   > **无代理的缓存够不到原站 → registry:2 把它当成 `404 blob unknown` 返回 → 拉取失败**。
   > 这正是早期用 1ms 时 **`make warm` 8 个里 7 个失败**的根因（不是"1ms 质量差"，也不是配置错，
   > 更不是免费额度——实测付费 token 下 quay 的 blob **照样 307**）。**换任何新上游前，先确认它
   > 自己中转 blob（blob 请求返回 200，而非 307 重定向到国外）**，否则这套缓存对 k8s/quay 必失败。

2. **Docker 的 `registry-mirror` 只对 docker.io 生效** → 对 k8s/quay/ghcr/gcr 无效。
   所以我们**不**靠 daemon 的 `registry-mirrors`，而是在 **kind 的 containerd** 里用 `certs.d/<registry>/hosts.toml`
   对每个上游单独做镜像重定向（见 `certs.d/`）。这样**无需改任何 chart 里的 image 名**。

3. **本机用 MonoCloudProxy（127.0.0.1:8118/8119）做代理，走代理的流量是计费的** →
   绝不能让镜像拉取走代理。两处保证：
   - **容器出网直连**：5 个缓存容器（registry:2）**不配任何代理**，访问 `*.m.daocloud.io`（国内）
     本就**直连、不走 VPN**。DaoCloud 自己中转 blob，所以缓存容器无需够到任何国外地址。
   - **Docker Desktop 守护进程代理**：Settings → Resources → Proxies → Manual，**Bypass** 列表加入
     `.m.daocloud.io`，让宿主侧任何直连 daocloud 的 `docker pull`（如 `make fetch` 逃生口）也绕开 VPN。

4. **kind 随时 create/delete，节点 containerd 里的镜像会随集群删除而丢失** →
   不能把镜像存在集群里。改为在**集群之外**跑 5 个持久化 `registry:2` **pull-through 缓存**
   （各带 docker named volume），每个对应一个上游 `*.m.daocloud.io`。
   首次拉取从 daocloud 落盘到缓存卷；之后**无限次重建 kind 都命中本地缓存（0 下载、可离线）**。

5. **多上游导致单一缓存会有路径冲突** → 每个上游一个独立缓存容器（命名空间天然隔离）。

### 数据流（最终形态）

```
Pod 拉 registry.k8s.io/coredns/coredns:vX
  └─ kind containerd 读 certs.d/registry.k8s.io/hosts.toml
       1) http://cache-k8s:5000        ← 命中缓存卷：0 下载、可离线 ✅
       2) https://k8s.m.daocloud.io     ← 未命中：daocloud 直连落盘（国内、不走 VPN，blob 自中转）
```

> **健壮性**：缓存是「优化」，`*.m.daocloud.io` 直连是「兜底」。某个缓存容器挂了，containerd 自动落到
> 第 2 个 host，最坏退化为「每次重建从 daocloud 重拉」，而不是部署失败。两级都在国内、都不烧 VPN。
>
> ⚠️ 若 DaoCloud 自身偶发缺某镜像 / 长期不可用：用**逃生口** `make fetch IMG="<完整镜像>"` —— 宿主
> 直连原始 registry（经 MonoProxy 代理，仅此镜像耗流量）拉取后 `kind load` 进集群。

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
├── Makefile             ← make up/down/warm/fetch/status/nuke
├── _common.sh           ← 公共变量/函数（缓存清单/上游在此）
├── registries.sh        ← 管理 5 个 pull-through 缓存
├── up.sh / down.sh      ← 集群拉起 / 拆除（down 保留缓存）
├── warm.sh + images.txt ← 预热镜像清单
├── fetch-direct.sh      ← 逃生口：直连原始 registry 拉取 + kind load
├── kind-cluster.yaml    ← kind 配置（containerd certs.d 重定向）
└── certs.d/             ← 每个上游一份 hosts.toml（缓存优先 → daocloud 兜底）
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
.m.daocloud.io,localhost,127.0.0.1,::1,.local,host.docker.internal,<内网私服IP或域名>,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16
```

→ **Apply & Restart**。含义：让守护进程拉 `*.m.daocloud.io`、内网仓库、本地地址时**直连**，其余仍可经 8118 兜底。
`<内网私服IP或域名>` 按本机实际内网镜像仓填写（如自建 registry、CNB `docker.cnb.cool` 等）；无则删除该项。

> 说明：核心 `make up`/`make warm` 流程里，缓存容器是**容器侧直连** daocloud，本就不经守护进程代理；
> 此 bypass 主要保障**宿主侧**直连 daocloud 的场景（如 `make fetch` 逃生口、手动 `docker pull docker.m.daocloud.io/...`）不绕道 VPN。
>
> ⚠️ 不要直接手改 `~/Library/Group Containers/group.com.docker/settings-store.json`——
> Docker Desktop 退出时会覆写。务必走 UI。

---

## 附录 B：排障（换机器 / 异常时按此分析）

| 症状 | 排查 |
|---|---|
| 拉镜像很慢/像在走代理 | 确认附录 A 的 bypass 含 `.m.daocloud.io` 且已 **Restart**；`make status` 看各缓存 `v2=200`。DaoCloud 免费站偶有限速，warm 幂等可重跑。 |
| `make status` 某缓存 `v2=000` | 该缓存容器没起来：`docker logs cache-xxx`；或上游域名变更，核对 `_common.sh` 的 `CACHES`。 |
| 拉取报 `unknown blob` / `blob unknown to registry`（404） | **上游不自己中转该 blob**。① 若上游是 daocloud：极少见，多为 daocloud 暂缺该镜像 → 逃生口 `make fetch IMG="<完整镜像>"`。② **若你把某缓存上游换成了 1ms 这类"重定向回原站"的镜像站**：k8s/quay/gcr/ghcr 的 blob 会被 307 甩回国外 CDN，无代理缓存够不到 → 必 404。**换回自己中转 blob 的上游（如 daocloud）**。详见 §0 第 1 点的⭐说明。 |
| Pod 报 `proxyconnect tcp: dial tcp 127.0.0.1:8118: connect: connection refused` | kind 把宿主 `HTTP(S)_PROXY` 注入了节点 containerd，而节点内 `127.0.0.1` 不是宿主、代理不可达。`up.sh` 已在 `kind create` 前 `env -u HTTP_PROXY ...` 剥离代理；若你手动建集群或改了 `up.sh`，务必同样剥离。验证：`docker exec <node> cat /etc/systemd/system/containerd.service.d/*.conf \| grep -i proxy` 应为空。 |
| Pod 一直 `ImagePullBackOff` | 1) `docker network inspect kind` 确认 `cache-*` 已接入（`make up` 会自动 connect）；2) 该镜像所在 registry 是否在 `certs.d/` 有对应目录；3) 看 `certs.d/<registry>/hosts.toml` 目录名是否与镜像 registry **完全一致**；4) `_catalog` 对 pull-through 缓存恒为空属正常，验证缓存命中改看 `docker exec cache-x ls /var/lib/registry/docker/registry/v2/repositories`。 |
| `make warm` 部分镜像失败、耗时很长 | DaoCloud 免费站 blob 可能限速/抖动。warm 幂等：**重跑只补未成功项**（失败行会打印上游真实报错）；持续失败的用 `make fetch` 逃生口。 |
| 新增了一个上游 registry（如 `docker.elastic.co`） | 新建 `certs.d/<registry>/hosts.toml`；在 `_common.sh` 的 `CACHES` 加一项指向 daocloud 对应子域名（`elastic.m.daocloud.io` 等），`make nuke && make up`。**务必确认新上游自己中转 blob**（§0⭐）。 |
| 磁盘报满 / `no space left` | Docker Desktop **Settings → Resources → Advanced** 调大 *Virtual disk limit*（默认偏小，本机磁盘充裕可设 200G+）。 |
| 私有镜像（如私有 ghcr）拉不动 | pull-through 缓存默认匿名；改用 `kind load`，或给对应 `cache-*` 加 `REGISTRY_PROXY_USERNAME/PASSWORD` 后 `make nuke && make up`。 |
| 跑 docker 命令时 macOS 反复弹「想访问其他 App 的数据」 | 从 IntelliJ 等 App 的终端运行时，`docker` 调 `credsStore`(desktop) 读 Docker Desktop 数据触发 TCC。本设施脚本已统一用**空 `DOCKER_CONFIG`**（`_common.sh`）规避；若你手动跑 `docker pull`，加 `DOCKER_CONFIG=$TMPDIR/hm-local-infra-docker` 前缀即可。 |
| 想换集群名/端口/上游 | `_common.sh` 改 `CLUSTER` / `CACHES`。改后 `make nuke && make up`。 |

参考：[kind 官方 registry 配置](https://kind.sigs.k8s.io/docs/user/local-registry/) ·
[containerd hosts.toml](https://github.com/containerd/containerd/blob/main/docs/hosts.md) ·
[DaoCloud public-image-mirror](https://github.com/DaoCloud/public-image-mirror)
