#!/usr/bin/env bash
# 管理 5 个持久化 pull-through 镜像缓存（registry:2 proxy → *.1ms.run）。
# 这些容器独立于 kind 生命周期：kind 反复 create/delete 不影响缓存卷。
#
# 用法：
#   ./registries.sh up        创建/启动全部缓存（幂等）
#   ./registries.sh connect   把缓存接入 kind docker 网络（kind 集群存在后才有意义）
#   ./registries.sh status    查看运行与健康状态
#   ./registries.sh down      停止并删除缓存容器（保留缓存卷 → 镜像不丢）
#   ./registries.sh nuke      连同缓存卷一并删除（彻底清空已下载镜像）
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
require docker

_up_one() {
  local name="$1" port="$2" upstream="$3" registry="$4"
  if docker inspect "$name" >/dev/null 2>&1; then
    docker start "$name" >/dev/null 2>&1 || true
    log "已存在 ${name}（上游 ${upstream}）→ 已确保运行"
    return
  fi
  log "创建 $name  ⇢  $upstream  （宿主调试端口 127.0.0.1:${port}）"
  docker run -d --restart=always --name "$name" \
    -p "127.0.0.1:${port}:5000" \
    -v "${name}:/var/lib/registry" \
    -e "REGISTRY_PROXY_REMOTEURL=${upstream}" \
    "$REGISTRY_IMAGE" >/dev/null
}

_connect_one() {
  local name="$1"
  docker inspect "$name" >/dev/null 2>&1 || return 0
  if docker network inspect "$KIND_NET" >/dev/null 2>&1; then
    docker network connect "$KIND_NET" "$name" 2>/dev/null \
      && log "接入网络：$name → $KIND_NET" \
      || true   # 已接入会报错，忽略
  else
    warn "docker 网络 '$KIND_NET' 不存在（kind 集群尚未创建）；创建集群后再 connect"
  fi
}

_status_one() {
  local name="$1" port="$2" upstream="$3"
  local state; state="$(docker inspect -f '{{.State.Status}}' "$name" 2>/dev/null || echo absent)"
  local code; code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:${port}/v2/" 2>/dev/null)"
  printf '  %-12s state=%-8s v2=%-4s upstream=%s\n' "$name" "$state" "$code" "$upstream"
}

_down_one() { docker rm -f "$1" >/dev/null 2>&1 && log "已删除容器 $1（卷保留）" || true; }
_nuke_one() { docker rm -f "$1" >/dev/null 2>&1 || true; docker volume rm "$1" >/dev/null 2>&1 && log "已删除卷 $1" || true; }

case "${1:-up}" in
  up)      each_cache _up_one; log "缓存就绪。若 kind 集群已存在，执行：$0 connect" ;;
  connect) each_cache _connect_one ;;
  status)  log "缓存状态："; each_cache _status_one ;;
  down)    each_cache _down_one ;;
  nuke)    each_cache _nuke_one ;;
  *)       die "未知子命令：$1（up|connect|status|down|nuke）" ;;
esac
