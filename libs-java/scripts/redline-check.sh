#!/usr/bin/env bash
# redline-check.sh —— 发布/CI 前的「信息红线」结构化校验。
#
# 本仓为公开开源仓（见主仓 CLAUDE.md）：POM/源码/脚本不得含任何甲方可识别信息、
# 真实主机 IP、内网地址或凭据。本脚本做两类检查，发现即非零退出：
#   1) 结构性：扫描真实 IPv4 字面量（排除回环/通配占位）。
#   2) denylist：可选本地词表 scripts/.redline-denylist（已 gitignore，仅本地留存真实代号/客户术语）。
#
# 退出码：0 干净；1 命中红线。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-$(cd "$SCRIPT_DIR/.." && pwd)}"   # 默认扫描 libs-java/
DENYLIST="$SCRIPT_DIR/.redline-denylist"

EXCLUDES=(--exclude-dir=target --exclude-dir=.git --exclude-dir=node_modules)
# 允许的占位/非敏感地址（回环、通配、文档示例域内出现的不计）
ALLOW_IP_RE='^(0\.0\.0\.0|127\.0\.0\.1|255\.255\.255\.255)$'

fail=0

echo "🔎 redline-check: 扫描 $TARGET"

# ---- 1) 真实 IPv4 字面量 ----
ip_hits="$(grep -RInaoE '\b([0-9]{1,3}\.){3}[0-9]{1,3}\b' "${EXCLUDES[@]}" "$TARGET" 2>/dev/null || true)"
if [[ -n "$ip_hits" ]]; then
  while IFS= read -r line; do
    ip="${line##*:}"
    if [[ ! "$ip" =~ $ALLOW_IP_RE ]]; then
      echo "  ❌ 疑似 IP 字面量: $line"
      fail=1
    fi
  done <<< "$ip_hits"
fi

# ---- 2) 本地 denylist（真实代号/客户术语，词表本身不入库） ----
if [[ -f "$DENYLIST" ]]; then
  while IFS= read -r term; do
    [[ -z "$term" || "$term" == \#* ]] && continue
    if grep -RInaF "${EXCLUDES[@]}" -- "$term" "$TARGET" >/dev/null 2>&1; then
      echo "  ❌ 命中 denylist 术语: $term"
      grep -RInaF "${EXCLUDES[@]}" -- "$term" "$TARGET" | sed 's/^/     /'
      fail=1
    fi
  done < "$DENYLIST"
else
  echo "  [info] 未发现本地 denylist: $DENYLIST ; 仅做结构性 IP 检查。"
fi

if [[ "$fail" -ne 0 ]]; then
  echo "🛑 redline-check 失败：请清除上述敏感信息后再发布。"
  exit 1
fi
echo "✅ redline-check 通过。"
