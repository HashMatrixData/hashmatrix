#!/usr/bin/env bash
# 逃生口：当某镜像在 daocloud 缺失（blob 404）或 daocloud 长期不可用时，
# 本设施的「缓存→*.m.daocloud.io」两级都指向 daocloud，拉不到。此脚本绕过 daocloud：
#   1) 在【宿主】直连【原始 registry】拉取 —— 原始 registry 不在 Docker Desktop 代理
#      Bypass 列表里，故自动走 MonoProxy(8118)。⚠️ 会消耗 VPN 流量，但仅限这一个镜像。
#   2) `kind load` 把镜像灌进集群节点 containerd —— 之后 Pod 命中本地、不再触网。
#
# 用法：./fetch-direct.sh <完整镜像引用> [<镜像2> ...]
#   ./fetch-direct.sh quay.io/jetstack/cert-manager-controller:v1.16.2
#
# 注意：kind load 的镜像随 `make down`(删集群)丢失；重建后对仍需的镜像再跑一次本脚本即可
#      （宿主已留有该镜像时第 1 步会自动跳过，不再走代理）。
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
require docker; require kind

[ "$#" -ge 1 ] || die "用法：$0 <镜像引用> [更多镜像...]"

cluster_up=0
kind get clusters 2>/dev/null | grep -qx "$CLUSTER" && cluster_up=1

for img in "$@"; do
  if docker image inspect "$img" >/dev/null 2>&1; then
    log "宿主已有镜像，跳过下载：$img"
  else
    log "宿主直连原始 registry 拉取（经代理，消耗 VPN 流量）：$img"
    docker pull "$img" || { warn "拉取失败：$img（原始 registry 是否需要代理/鉴权？）"; continue; }
  fi
  if [ "$cluster_up" = 1 ]; then
    log "载入集群：kind load $img → $CLUSTER"
    kind load docker-image "$img" --name "$CLUSTER"
  else
    warn "集群 $CLUSTER 不存在；镜像已在宿主，建集群后重跑本脚本即可载入"
  fi
done

log "完成。若此前 Pod 处于 ImagePullBackOff，载入后会自动恢复（imagePullPolicy=IfNotPresent）。"
