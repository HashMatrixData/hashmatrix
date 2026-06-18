#!/usr/bin/env bash
# mirror-to-nexus.sh —— 内网/信创交付：把已发布制品镜像同步到内网 Maven 私服（Nexus/Artifactory）。
#
# 背景：制品仓 = GitHub Packages（公网）。内网无公网，须把 parent/bom/starter-* 镜像到内网私服，
# 子仓在内网指向私服即可构建（见 libs-java/README「内网/信创交付」）。本脚本幂等、可重跑。
#
# 典型链路（两段式，跨网闸）：
#   联网侧：mvn -f libs-java/pom.xml install        # 或 dependency:get 从 GitHub Packages 拉到本地仓
#           ./mirror-to-nexus.sh --bundle out/      # 导出制品成离线包，过网闸
#   内网侧：NEXUS_URL=... NEXUS_REPO_ID=... ./mirror-to-nexus.sh --from out/   # deploy-file 到私服
#
# 凭据/地址：经环境变量 + ~/.m2/settings.xml 的 server（id=$NEXUS_REPO_ID）注入，绝不入库（红线）。
#
# 环境变量：
#   VERSION       默认读 libs-java/pom.xml 当前版本
#   NEXUS_URL     内网私服 release 仓 URL（--from 模式必填）
#   NEXUS_REPO_ID settings.xml 中对应 server 的 id（默认 internal-nexus）
#   LOCAL_REPO    本地 Maven 仓（默认 ~/.m2/repository）
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
LIBS="$ROOT/libs-java"
GROUP_PATH="io/hashmatrix"
LOCAL_REPO="${LOCAL_REPO:-$HOME/.m2/repository}"
NEXUS_REPO_ID="${NEXUS_REPO_ID:-internal-nexus}"

# 发布物清单：pom 模块 + jar 模块
POM_ARTIFACTS=(hashmatrix-platform-parent hashmatrix-bom)
JAR_ARTIFACTS=(hashmatrix-starter-tenant hashmatrix-starter-web hashmatrix-starter-test)

MODE="" ; BUNDLE_DIR="" ; FROM_DIR=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --bundle) MODE=bundle; BUNDLE_DIR="${2:?--bundle 需要目录}"; shift 2 ;;
    --from)   MODE=from;   FROM_DIR="${2:?--from 需要目录}"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "未知参数: $1" >&2; exit 2 ;;
  esac
done

VERSION="${VERSION:-$(mvn -q -f "$LIBS/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null)}"
echo "▶ mirror-to-nexus: version=$VERSION mode=${MODE:-未指定}"

src_dir() { echo "$LOCAL_REPO/$GROUP_PATH/$1/$VERSION"; }

case "$MODE" in
  bundle)
    # 联网侧：从本地仓导出离线包
    mkdir -p "$BUNDLE_DIR"
    for a in "${POM_ARTIFACTS[@]}" "${JAR_ARTIFACTS[@]}"; do
      d="$(src_dir "$a")"
      [[ -d "$d" ]] || { echo "缺少制品 $a:$VERSION（先 mvn install 或 dependency:get）" >&2; exit 1; }
      mkdir -p "$BUNDLE_DIR/$a"
      cp "$d/$a-$VERSION".* "$BUNDLE_DIR/$a/" 2>/dev/null || true
    done
    echo "✅ 已导出到 $BUNDLE_DIR，过网闸后在内网侧用 --from 同步。"
    ;;

  from)
    : "${NEXUS_URL:?--from 模式需设置 NEXUS_URL（内网私服 release 仓）}"
    deploy_file() { # artifact packaging
      local a="$1" pkg="$2" base="$FROM_DIR/$a/$a-$VERSION"
      local pom="$base.pom" file="$base.$pkg"
      [[ "$pkg" == "pom" ]] && file="$pom"
      [[ -f "$file" ]] || { echo "缺少文件 $file" >&2; exit 1; }
      mvn -B -ntp org.apache.maven.plugins:maven-deploy-plugin:deploy-file \
        -DrepositoryId="$NEXUS_REPO_ID" -Durl="$NEXUS_URL" \
        -DgroupId=io.hashmatrix -DartifactId="$a" -Dversion="$VERSION" \
        -Dpackaging="$pkg" -Dfile="$file" -DpomFile="$pom"
    }
    for a in "${POM_ARTIFACTS[@]}"; do deploy_file "$a" pom; done
    for a in "${JAR_ARTIFACTS[@]}"; do deploy_file "$a" jar; done
    echo "✅ 已镜像到 $NEXUS_URL（repoId=$NEXUS_REPO_ID）。内网子仓在 settings.xml 指向该私服即可构建。"
    ;;

  *)
    echo "请指定 --bundle <dir>（联网侧导出）或 --from <dir>（内网侧同步）。-h 查看帮助。" >&2
    exit 2
    ;;
esac
