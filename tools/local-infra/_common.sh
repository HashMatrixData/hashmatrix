#!/usr/bin/env bash
# 公共变量与函数（被 registries.sh / up.sh / down.sh / warm.sh 复用）。
# 兼容 macOS 自带 bash 3.2：不使用关联数组。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLUSTER="${HM_KIND_CLUSTER:-hashmatrix-local}"   # 与 kind-cluster.yaml 的 name 一致
KIND_NET="kind"                                  # kind 创建集群时使用的 docker 网络
REGISTRY_IMAGE="${HM_REGISTRY_IMAGE:-registry:2}"
CERTS_D="${SCRIPT_DIR}/certs.d"

# 用一个【空】DOCKER_CONFIG，避免 docker CLI 调起 credsStore（docker-credential-desktop）
# 去读 Docker Desktop 数据 —— 从 IntelliJ 等 App 的终端运行时，macOS(26+) 会就每次 pull 弹
# “想访问其他 App 数据”授权窗。本设施只拉公共/匿名镜像，空配置足够；不影响你的 ~/.docker。
export DOCKER_CONFIG="${HM_DOCKER_CONFIG:-${TMPDIR:-/tmp}/hm-local-infra-docker}"
mkdir -p "$DOCKER_CONFIG" 2>/dev/null || true
[ -f "$DOCKER_CONFIG/config.json" ] || printf '{}' > "$DOCKER_CONFIG/config.json" 2>/dev/null || true

# 缓存定义：name|宿主端口(127.0.0.1)|上游(DaoCloud 镜像)|对应原始 registry
# 端口仅用于宿主侧调试/预热；kind 内部通过容器名 cache-x:5000 访问。
# 上游选 DaoCloud（m.daocloud.io）：它对所有 registry 的 blob 都【自己中转(200)】，
# 故无代理的缓存容器能直接落盘、不走 VPN。详见 README「前因后果」（1ms 对 k8s/quay
# 的 blob 是 307 重定向回国外原站 CDN，无代理缓存够不到 → 这正是早期 7/8 失败的根因）。
CACHES="cache-docker|5001|https://docker.m.daocloud.io|docker.io
cache-k8s|5002|https://k8s.m.daocloud.io|registry.k8s.io
cache-quay|5003|https://quay.m.daocloud.io|quay.io
cache-ghcr|5004|https://ghcr.m.daocloud.io|ghcr.io
cache-gcr|5005|https://gcr.m.daocloud.io|gcr.io"

log()  { printf '\033[1;34m[local-infra]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[local-infra]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[local-infra] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# 遍历每个缓存，调用 $1 name port upstream registry
each_cache() {
  local fn="$1" name port upstream registry
  while IFS='|' read -r name port upstream registry; do
    [ -n "$name" ] && "$fn" "$name" "$port" "$upstream" "$registry"
  done <<EOF
$CACHES
EOF
}

require() { command -v "$1" >/dev/null 2>&1 || die "缺少依赖：$1（请先安装）"; }
