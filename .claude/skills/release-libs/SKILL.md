---
name: release-libs
description: 一键发布 libs-java 公共依赖（platform-parent / bom / starter-*）到 GitHub Packages——版本递增→构建验证→打 tag→CI 发布，并含内网私服镜像同步与发布前红线校验。幂等可重跑。当用户要求发布公共依赖、升 BOM 版本、release libs-java 时使用。
argument-hint: "[新版本号，如 0.2.0  或  --bump patch|minor|major]"
---

# release-libs · 公共依赖发布工作流

把 `libs-java` 的 `parent/bom/starter-*` 一键发布为带版本制品到 **GitHub Packages**，并支持**内网/信创**私服镜像同步。**硬门控**：发布前必过红线校验；全流程**幂等可重跑**（失败可从中断处再跑，已存在的 tag 自动跳过）。

> 配套：`libs-java/scripts/release.sh`（准备+打 tag）、`mirror-to-nexus.sh`（内网镜像）、`redline-check.sh`（红线）、`.github/workflows/libs-java-release.yml`（tag 触发的 CI 发布）。

## Step 1: 明确版本

- 入参为显式版本（如 `0.2.0`）或 `--bump patch|minor|major`。模糊时**先问**用户要发的版本与变更摘要。
- 语义化版本：BOM 钉死的框架/库升级或 starter 行为变更 → minor；纯修复 → patch；不兼容 → major。
- 确认工作区干净（`git status`），当前分支为目标发布分支。

## Step 2: 发布前红线校验（硬门控）⭐

```bash
bash libs-java/scripts/redline-check.sh
```

- **非零退出即停**：制品/POM/源码不得含甲方信息、真实 IP、内网地址、凭据（见主仓 `CLAUDE.md`）。
- 真实代号/客户术语可放本地 `libs-java/scripts/.redline-denylist`（已 gitignore），脚本会一并扫描。
- 红线未过**不得继续**。

## Step 3: 递增版本 + 构建验证 + 打 tag

```bash
# 显式版本：
bash libs-java/scripts/release.sh 0.2.0
# 或自动递增：
bash libs-java/scripts/release.sh --bump patch
```

脚本依次完成（幂等）：① 红线校验；② `versions:set` 统一升 reactor（parent/bom/starter-*）+ 同步样例；③ `-Prelease clean verify`（全测试 + source/javadoc）；④ 写 `libs-java/CHANGELOG.md`；⑤ 提交 + 打 `libs-java-v<version>` tag。

- **补 CHANGELOG**：把脚本写入的 `TODO` 摘要替换为真实变更要点（**不含客户/凭据信息**）。
- 若中途失败：修复后**重跑同一命令**即可（`versions:set` 幂等；tag 已存在则提示并退出）。

## Step 4: 触发 GitHub Packages 发布

```bash
git push && git push origin libs-java-v<version>
# 或在 Step 3 直接： release.sh <version> --push
```

- 推送 tag 触发 `libs-java-release.yml`：用 `GITHUB_TOKEN`（`packages: write`）`mvn -Prelease deploy` 发布 `parent/bom/starter-*`，并建 GitHub Release。
- 校验：GitHub → Packages 出现对应版本；`HashMatrixData/hashmatrix` Releases 有 `libs-java-v<version>`。

## Step 5: 内网/信创私服镜像同步（交付期）

内网无公网，须把制品镜像到内网 Nexus/Artifactory：

```bash
# 联网侧：导出离线包（先确保本地仓有该版本：mvn -f libs-java/pom.xml install）
bash libs-java/scripts/mirror-to-nexus.sh --bundle out/
# 过网闸后，内网侧：
NEXUS_URL=<内网release仓URL> NEXUS_REPO_ID=internal-nexus \
  bash libs-java/scripts/mirror-to-nexus.sh --from out/
```

- `NEXUS_URL`/凭据经环境变量 + `~/.m2/settings.xml` 注入，**绝不入库**（红线）。
- 内网子仓在 `settings.xml` 指向该私服（或用 `-Pxinchuang` profile 切镜像）即可只 clone 子仓构建。

## Step 6: 收尾

- 汇总：发布版本、制品清单、Release 链接、（如适用）内网镜像结果。
- 提示子仓升级方式：改一行 BOM `import` 版本号即可。
- 若源于 Issue，PR/说明可关联对应 Issue。
