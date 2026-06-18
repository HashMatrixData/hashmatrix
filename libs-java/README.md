# libs-java · 公共 Java 依赖

主仓统一的 Java 公共能力。**子仓通过 Maven 坐标引用**（非 submodule 路径）——只 clone 子仓、能访问制品仓即可 `mvn` 构建。

| 制品 | 坐标 | 作用 |
|--|--|--|
| platform-parent | `io.hashmatrix:hashmatrix-platform-parent` (pom) | 统一 **Java 版本 / 编码 / 插件管理 / 编译·质量门 / profile(oss·信创) / 发布配置** |
| bom | `io.hashmatrix:hashmatrix-bom` (pom) | `dependencyManagement` **钉死开发框架版本**（Spring Boot 家族 + 测试栈 + starter）——版本**唯一来源** |
| starter-tenant | `io.hashmatrix:hashmatrix-starter-tenant` | 多租户上下文 `TenantContext`（X-Tenant-* 头 → ThreadLocal，架构 05 §5） |
| starter-web | `io.hashmatrix:hashmatrix-starter-web` | Web 基座：统一返回 `ApiResponse` + 全局异常处理 |
| starter-test | `io.hashmatrix:hashmatrix-starter-test` | 统一测试栈（JUnit5 + AssertJ + Mockito + Testcontainers）+ 脱敏 fixtures |

> **基线**：Java 17 · Spring Boot 3.3.5（经 BOM 钉死，升级=改 BOM 一行）。
> 路线图（后续 starter）：日志 / 审计 / 鉴权——新增即纳入 BOM 管理，沿用本目录模式。
> 当前版本：见各 `pom.xml`（统一版本，由 [release-libs](#发布) 递增）。

## 目录

```
libs-java/
├── pom.xml                 # hashmatrix-platform-parent（聚合器 + parent）
├── bom/                    # hashmatrix-bom
├── starter-tenant/         # TenantContext / Filter / AutoConfiguration
├── starter-web/            # ApiResponse / GlobalExceptionHandler
├── starter-test/           # 测试栈 + fixtures（MockTenants / MockData）
├── examples/sample-service/# 子仓接入样例（不入 reactor，仅经坐标消费）
└── scripts/                # release.sh / mirror-to-nexus.sh / redline-check.sh
```

## 子仓接入（标准姿势）

`<parent>` 引用 parent + `import` BOM；依赖**不写版本号**（由 BOM 钉死）：

```xml
<parent>
  <groupId>io.hashmatrix</groupId>
  <artifactId>hashmatrix-platform-parent</artifactId>
  <version>0.1.0</version>
  <relativePath/>           <!-- 留空：从制品仓解析，不依赖路径 -->
</parent>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.hashmatrix</groupId>
      <artifactId>hashmatrix-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.hashmatrix</groupId>
    <artifactId>hashmatrix-starter-tenant</artifactId>
  </dependency>
  <dependency>
    <groupId>io.hashmatrix</groupId>
    <artifactId>hashmatrix-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>io.hashmatrix</groupId>
    <artifactId>hashmatrix-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

完整可运行示例见 [`examples/sample-service`](./examples/sample-service)。

### 能力速览

- **starter-tenant**：网关注入 `X-Tenant-Id`/`X-Tenant-Org` → `TenantContextFilter` 绑定 `TenantContextHolder`。业务/数据访问层 `TenantContextHolder.requireTenantId()` 取当前租户做 schema/catalog 路由。跨线程用 `TenantContextHolder.callWith(ctx, ...)` 显式传播。配置前缀 `hashmatrix.tenant.*`（`header`/`required`/`filter-order`/`enabled`）。
- **starter-web**：控制器返回 `ApiResponse.ok(data)`；抛 `BusinessException(status, code, msg)`，由 `GlobalExceptionHandler` 统一转 `ApiResponse`（未知异常→500 且不泄露细节）。开关 `hashmatrix.web.exception-handler.enabled`。
- **starter-test**：以 `test` 作用域引入即获完整测试栈；租户取 `MockTenants`，样例数据取 `MockData`（确定性、脱敏：`@example.com` / `*.example.internal`）。新增 fixtures 须守红线。

## 验证「只 clone 子仓可构建」

样例**刻意不在 reactor**，模拟独立子仓——先把公共制品装进本地仓，再仅经坐标构建样例：

```bash
mvn -f libs-java/pom.xml clean install                       # 安装 parent/bom/starter-*
mvn -f libs-java/examples/sample-service/pom.xml clean verify # 零路径依赖，仅从仓库解析
# DoD 形式：mvn -q -f libs-java/examples/sample-service/pom.xml -DskipTests package
```

CI（`.github/workflows/libs-java-ci.yml`）每次 PR 都跑这两步，持续守住「只 clone 子仓可构建」契约。

## 本地构建

需 **JDK 17 + Maven 3.8+**（由 enforcer 强制）。

```bash
mvn -f libs-java/pom.xml clean verify      # 编译 + 测试 + 质量门
mvn -f libs-java/pom.xml clean install     # 装进本地仓供下游/样例消费
```

## 访问制品仓（GitHub Packages）

制品仓 = **GitHub Packages**（`https://maven.pkg.github.com/HashMatrixData/hashmatrix`）。拉取需在 `~/.m2/settings.xml` 配置带 `read:packages` 的 PAT：

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>${env.GITHUB_TOKEN}</password>   <!-- read:packages，经环境变量注入，勿写明文 -->
  </server>
</servers>
```

并在子仓 `pom.xml` 或 `settings.xml` 声明 `id=github` 的 `<repository>` 指向上述 URL。

## 内网 / 信创交付：制品镜像同步

内网无公网，须把 GitHub Packages 制品**镜像到内网 Maven 私服（Nexus/Artifactory）**，内网子仓指向私服：

```bash
# ① 联网侧导出离线包（先确保本地仓有目标版本）
mvn -f libs-java/pom.xml install
bash libs-java/scripts/mirror-to-nexus.sh --bundle out/

# ② 过网闸后，内网侧 deploy 到私服（地址/凭据经 env + settings.xml 注入，不入库）
NEXUS_URL=<内网 release 仓 URL> NEXUS_REPO_ID=internal-nexus \
  bash libs-java/scripts/mirror-to-nexus.sh --from out/
```

内网构建可用 `-Pxinchuang` profile 将仓库切到内网私服（`${hashmatrix.mirror.url}` 注入，仅切镜像不改逻辑，呼应架构 04 §3）。

## 版本演进

主仓 CI 发布 BOM `x.y.z` → 子仓 pin；**升级 = 改一行版本号**。全模块同版本，由发布脚本统一递增。

## 发布

由主仓 [`release-libs` SKILL](../.claude/skills/release-libs/SKILL.md) 一键完成：红线校验 → 版本递增 → 构建验证 → 打 tag → CI 发布到 GitHub Packages（+ Release/changelog）→ 内网私服镜像同步。

```bash
bash libs-java/scripts/release.sh 0.2.0 --push     # 或 --bump patch
```

幂等可重跑；发布前 `redline-check.sh` 硬门控（制品/POM 无客户信息）。

## 🔴 红线

公开开源仓：POM/源码/脚本/示例**不得含任何甲方信息、真实 IP、内网地址、凭据**；示例数据一律脱敏占位（`acme` / `tenant-demo` / `example.com` / `example.internal`）。详见主仓 [`CLAUDE.md`](../CLAUDE.md)。
