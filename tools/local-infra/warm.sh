#!/usr/bin/env bash
# 预热：把 images.txt 中的镜像通过对应缓存拉一遍，使缓存卷落盘。
# 之后 kind 内拉取同一镜像将命中缓存（0 下载）。拉取后删除宿主侧标签，
# 避免污染宿主 docker image 库（缓存卷内 blob 不受影响）。
#
# 用法：./warm.sh [images.txt]
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
require docker

LIST="${1:-${SCRIPT_DIR}/images.txt}"
[ -f "$LIST" ] || die "找不到清单文件：$LIST"

port_for() {
  case "$1" in
    docker.io)       echo 5001 ;;
    registry.k8s.io) echo 5002 ;;
    quay.io)         echo 5003 ;;
    ghcr.io)         echo 5004 ;;
    gcr.io)          echo 5005 ;;
    *)               echo "" ;;
  esac
}

ok=0; fail=0; skip=0
while IFS= read -r raw || [ -n "$raw" ]; do
  ref="$(printf '%s' "$raw" | sed 's/#.*//; s/^[[:space:]]*//; s/[[:space:]]*$//')"
  [ -z "$ref" ] && continue

  first="${ref%%/*}"
  if printf '%s' "$first" | grep -qE '[.:]' || [ "$first" = localhost ]; then
    registry="$first"; repo="${ref#*/}"
  else
    registry="docker.io"
    case "$ref" in */*) repo="$ref" ;; *) repo="library/$ref" ;; esac
  fi

  port="$(port_for "$registry")"
  if [ -z "$port" ]; then
    warn "跳过（无对应缓存的 registry：${registry}）：${ref}"; skip=$((skip+1)); continue
  fi

  target="127.0.0.1:${port}/${repo}"
  log "预热 $ref  ⇢  $target"
  pulled=0
  for attempt in 1 2 3; do   # 容忍缓存冷启动 / 上游瞬时抖动
    if docker pull "$target" >/dev/null 2>&1; then pulled=1; break; fi
    sleep 2
  done
  if [ "$pulled" = 1 ]; then
    docker rmi "$target" >/dev/null 2>&1 || true   # 仅删宿主标签，缓存卷保留
    ok=$((ok+1))
  else
    warn "失败：${ref}（检查 tag 是否存在 / 上游 1ms 是否可达）"; fail=$((fail+1))
  fi
done < "$LIST"

log "预热完成：成功 $ok · 失败 $fail · 跳过 $skip"
[ "$fail" -eq 0 ]
