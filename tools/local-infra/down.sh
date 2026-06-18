#!/usr/bin/env bash
# 拆除 kind 集群，但【保留】镜像缓存容器与卷 → 下次 up 不重新下载镜像。
# 要连缓存一起清空，用：./registries.sh nuke
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
require kind

if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  log "删除 kind 集群 '${CLUSTER}'（缓存保留）"
  kind delete cluster --name "$CLUSTER"
else
  log "集群 '${CLUSTER}' 不存在，无需删除"
fi
log "缓存仍在运行（./registries.sh status 查看）。重新拉起：./up.sh"
