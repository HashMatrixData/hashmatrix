#!/usr/bin/env bash
# 公共变量与函数（被 registries.sh / up.sh / down.sh / warm.sh 复用）。
# 兼容 macOS 自带 bash 3.2：不使用关联数组。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLUSTER="${HM_KIND_CLUSTER:-hashmatrix-local}"   # 与 kind-cluster.yaml 的 name 一致
KIND_NET="kind"                                  # kind 创建集群时使用的 docker 网络
REGISTRY_IMAGE="${HM_REGISTRY_IMAGE:-registry:2}"
CERTS_D="${SCRIPT_DIR}/certs.d"

# 缓存定义：name|宿主端口(127.0.0.1)|上游(1ms)|对应原始 registry
# 端口仅用于宿主侧调试/预热；kind 内部通过容器名 cache-x:5000 访问。
CACHES="cache-docker|5001|https://docker.1ms.run|docker.io
cache-k8s|5002|https://k8s.1ms.run|registry.k8s.io
cache-quay|5003|https://quay.1ms.run|quay.io
cache-ghcr|5004|https://ghcr.1ms.run|ghcr.io
cache-gcr|5005|https://gcr.1ms.run|gcr.io"

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
