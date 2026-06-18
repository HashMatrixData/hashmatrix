#!/usr/bin/env bash
# 一键拉起本地测试基础设施：5 个镜像缓存 + kind 集群（containerd 走 DaoCloud 镜像缓存）。
# 幂等：缓存/集群已存在则复用，不重复下载。
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"
require docker; require kind; require kubectl

# 1) 缓存（独立于 kind，持久）
log "==> [1/4] 确保镜像缓存运行"
"${SCRIPT_DIR}/registries.sh" up

# 2) kind 集群（若不存在则创建）
log "==> [2/4] 确保 kind 集群 '${CLUSTER}'"
if kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  log "集群已存在，跳过创建"
else
  tmp="$(mktemp -t kind-${CLUSTER}.XXXXXX.yaml)"
  sed "s|__CERTS_D__|${CERTS_D}|g" "${SCRIPT_DIR}/kind-cluster.yaml" > "$tmp"
  log "创建集群（certs.d=${CERTS_D}）"
  # 关键：剥离宿主代理环境再建集群。否则 kind 会把宿主的 HTTP(S)_PROXY=127.0.0.1:8118
  # 注入到节点 containerd —— 而节点内的 127.0.0.1 并非宿主，代理永不可达，
  # 导致一切镜像拉取（含走 cache-*/daocloud 的）proxyconnect 失败。
  # 本设计下节点拉镜像全部经 cache-*（kind 网内直连）或 *.m.daocloud.io（直连），无需任何代理。
  env -u HTTP_PROXY -u HTTPS_PROXY -u http_proxy -u https_proxy -u NO_PROXY -u no_proxy \
    kind create cluster --config "$tmp"
  rm -f "$tmp"
fi

# 3) 把缓存接入 kind 网络（此时网络已存在）
log "==> [3/4] 接入缓存到 kind 网络"
"${SCRIPT_DIR}/registries.sh" connect

# 4) 等待节点就绪
log "==> [4/4] 等待节点 Ready"
kubectl --context "kind-${CLUSTER}" wait --for=condition=Ready nodes --all --timeout=120s || \
  warn "节点未在 120s 内 Ready，请检查 'kubectl get nodes'"

log "完成 ✅  kubectl context = kind-${CLUSTER}"
log "验证镜像走缓存：kubectl run t --image=registry.k8s.io/pause:3.10 --restart=Never && kubectl get pod t -w"
