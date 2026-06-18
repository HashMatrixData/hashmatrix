#!/usr/bin/env bash
# release.sh —— libs-java 公共依赖发布驱动（幂等、可重跑）。
#
# 流程：红线校验 → 版本递增（reactor + 样例） → 构建验证 → CHANGELOG → 提交 + 打 tag → (可选) push。
# 实际「发布到 GitHub Packages」由 push tag 触发的 .github/workflows/libs-java-release.yml 完成
# （用 GITHUB_TOKEN，免本地配私钥）。本脚本只做「准备 + 打 tag」，故可安全重跑。
#
# 用法：
#   release.sh <new-version>          # 显式版本，如 0.2.0
#   release.sh --bump patch|minor|major
#   附加 --push 同时推送分支与 tag（触发 CI 发布）
#
# 前置：JDK 17、Maven、干净工作区（或 --allow-dirty）。
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
LIBS="$ROOT/libs-java"
EXAMPLE="$LIBS/examples/sample-service/pom.xml"
SCRIPT_DIR="$LIBS/scripts"

PUSH=0
ALLOW_DIRTY=0
VERSION_ARG=""
BUMP=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push) PUSH=1; shift ;;
    --allow-dirty) ALLOW_DIRTY=1; shift ;;
    --bump) BUMP="${2:?--bump 需要 patch|minor|major}"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    -*) echo "未知参数: $1" >&2; exit 2 ;;
    *) VERSION_ARG="$1"; shift ;;
  esac
done

current_version() {
  mvn -q -f "$LIBS/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null
}

bump_version() {
  local cur="$1" kind="$2" major minor patch
  IFS='.' read -r major minor patch <<< "${cur%%-*}"
  case "$kind" in
    major) echo "$((major+1)).0.0" ;;
    minor) echo "${major}.$((minor+1)).0" ;;
    patch) echo "${major}.${minor}.$((patch+1))" ;;
    *) echo "无效 --bump: $kind" >&2; exit 2 ;;
  esac
}

OLD="$(current_version)"
if [[ -n "$BUMP" ]]; then
  NEW="$(bump_version "$OLD" "$BUMP")"
elif [[ -n "$VERSION_ARG" ]]; then
  NEW="$VERSION_ARG"
else
  echo "需要 <new-version> 或 --bump <kind>（当前版本 $OLD）" >&2
  exit 2
fi

# 严格三段语义化版本 + 可选预发布后缀；拒绝四段/前导零
if ! [[ "$NEW" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z.-]+)?$ ]]; then
  echo "版本号非法: $NEW（须为 x.y.z 或 x.y.z-pre）" >&2; exit 2
fi

TAG="libs-java-v$NEW"
echo "▶ 发布 libs-java: $OLD → $NEW  (tag: $TAG)"

# 幂等：tag 已存在则视为已发布，直接退出
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  echo "✅ tag $TAG 已存在，无需重复准备。如需触发 CI 发布：git push origin $TAG"
  exit 0
fi

if [[ "$ALLOW_DIRTY" -eq 0 && -n "$(git status --porcelain)" ]]; then
  echo "🛑 工作区不干净，请先提交/暂存（或加 --allow-dirty）。" >&2
  exit 1
fi

# 1) 红线校验（发布前硬门控）
bash "$SCRIPT_DIR/redline-check.sh"

# 2) 版本递增：reactor（parent/bom/starter-*）+ 不在 reactor 的样例
mvn -q -f "$LIBS/pom.xml" versions:set -DnewVersion="$NEW" -DgenerateBackupPoms=false
perl -0pi -e "s/>\Q$OLD\E</>$NEW</g" "$EXAMPLE"

# 3) 构建验证（含 source/javadoc 与全部测试）
mvn -B -ntp -f "$LIBS/pom.xml" -Prelease clean verify

# 4) CHANGELOG（幂等：同版本不重复插入）
CHANGELOG="$LIBS/CHANGELOG.md"
[[ -f "$CHANGELOG" ]] || printf '# libs-java Changelog\n\n' > "$CHANGELOG"
if ! grep -q "## $TAG " "$CHANGELOG"; then
  tmp="$(mktemp)"
  { head -n 2 "$CHANGELOG"
    printf '## %s — %s\n\n- TODO: 本次变更摘要（不含客户/凭据信息）。\n\n' "$TAG" "$(date +%F)"
    tail -n +3 "$CHANGELOG"
  } > "$tmp"
  mv "$tmp" "$CHANGELOG"
fi

# 5) 提交 + 打 tag
git -C "$ROOT" add libs-java
if ! git -C "$ROOT" diff --cached --quiet; then
  git -C "$ROOT" commit -m "chore(libs-java): release v$NEW"
fi
git -C "$ROOT" tag -a "$TAG" -m "libs-java v$NEW"
echo "✅ 已提交并打 tag $TAG"

if [[ "$PUSH" -eq 1 ]]; then
  git -C "$ROOT" push
  git -C "$ROOT" push origin "$TAG"
  echo "🚀 已推送，CI（libs-java-release.yml）将发布到 GitHub Packages。"
else
  echo "ℹ️  未推送。触发 CI 发布：git push && git push origin $TAG"
fi
