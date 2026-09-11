# 发布主题库到远程 Maven（GitHub Packages）

## 目标
把 `skinlibrary`（核心换肤 SDK）发布到 GitHub Packages，其他项目通过远程 Maven 依赖直接使用，无需拷贝源码或本地 AAR。

## 背景与关键结论
- `skinlibrary` 只依赖公共 Maven 依赖（appcompat / constraintlayout / core-ktx / material / compose BOM+ui+runtime / MMKV），**不依赖** `bydrepo` 或 BYD SDK，也无 `project(:)` 依赖 → 可独立发布，下游无 BYD 依赖解析问题。
- `bydwidget`、`skinpackage`、BYD SDK 本次**不发布**（按用户选择）。
- GitHub Packages Maven 是**单一仓库 URL**：`https://maven.pkg.github.com/kylin0628/SkinDemo`。
  - snapshot / release 靠版本后缀区分：`1.0.0-SNAPSHOT` 可覆盖（迭代），`1.0.0` 不可覆盖（稳定发布）。
- `skinlibrary/src/main/assets/skin/skindemo.skin` 会被打进 AAR → 下游直接依赖即内置默认皮肤，无需额外下发。

## 变更清单

### 1. `skinlibrary/build.gradle` — 发布配置 + 依赖 scope 修正
- 新增 `apply plugin: 'maven-publish'`。
- 顶部加：
  - `group = 'com.kylin'`
  - `version = (project.findProperty('skinVersion') ?: System.getenv('SKIN_VERSION') ?: '1.0.0-SNAPSHOT')`
- 新增 `publishing` 块：
  - `publications.release(MavenPublication)`：`from components.release`（AGP 自动生成 AAR + POM，packaging=aar，`api`→compile、`implementation`→runtime 自动映射）。
  - `groupId 'com.kylin'`、`artifactId 'skinlibrary'`、`version project.version`。
  - `repositories.maven`：url `https://maven.pkg.github.com/kylin0628/SkinDemo`，credentials 读 `gpr.user`/`gpr.key`（项目属性或环境变量 `GPR_USER`/`GPR_KEY`）。
- **依赖 scope 修正**（公开 API 泄漏类型必须 `api`，否则下游编译失败）：
  - `appcompat`、`constraintlayout`、`core-ktx`、`material` → `api`
  - `compose-bom`(platform)、`compose.ui`、`compose.runtime` → `api`
  - **新增** `api 'androidx.viewpager:viewpager:1.0.0'`（`SkinnableViewPager` 泄漏 `ViewPager`，appcompat 1.6.1 已不传递）
  - `mmkv` **保持** `implementation`（`KvStore` 已完全封装，不泄漏 MMKV 类型）

### 2. 新增 `publish.sh` — 一键发布
- 用法：
  - `./publish.sh` → 发布 release（`1.0.0` 或 `SKIN_VERSION` 指定）
  - `./publish.sh snapshot` → 发布 `1.0.0-SNAPSHOT`（可覆盖迭代）
- 内部执行 `./gradlew :skinlibrary:publish -PskinVersion=<版本>`，发布前先跑 `:skinlibrary:assembleRelease` 确保皮肤包同步。
- 从环境变量 `GPR_USER` / `GPR_KEY`（或 `~/.gradle/gradle.properties` 的 `gpr.user`/`gpr.key`）读取凭证；缺失时明确报错提示。

### 3. 凭证说明（不提交进 git）
- 生成 GitHub PAT（权限 `write:packages` + `read:packages`）。
- 建议写入 `~/.gradle/gradle.properties`：
  ```
  gpr.user=<GitHub 用户名>
  gpr.key=<PAT>
  ```
  或用环境变量 `GPR_USER` / `GPR_KEY`。
- 项目内被跟踪的 `gradle.properties` **不放** token。

### 4. `README.md` — 新增「远程依赖使用」章节
下游项目接入示例：
```gradle
// settings.gradle 的 dependencyResolutionManagement 或模块 repositories
maven {
    url = uri("https://maven.pkg.github.com/kylin0628/SkinDemo")
    credentials {
        username = project.findProperty("gpr.user") ?: System.getenv("GPR_USER")
        password = project.findProperty("gpr.key") ?: System.getenv("GPR_KEY")
    }
}

dependencies {
    implementation 'com.kylin:skinlibrary:1.0.0'          // 稳定版
    // implementation 'com.kylin:skinlibrary:1.0.0-SNAPSHOT'  // 快照版
}
```
说明：下游需在 `~/.gradle/gradle.properties` 配 `gpr.user`/`gpr.key`（PAT 需 `read:packages`）。

## 验证方式
1. `./publish.sh snapshot` 发布快照版成功。
2. 新建临时下游工程，按 README 配仓库 + 依赖 `com.kylin:skinlibrary:1.0.0-SNAPSHOT`，`SkinActivity` / `skinnedColor()` 能编译通过（验证 `api` scope 正确）。
3. 运行下游 Demo，确认默认皮肤随 AAR assets 自动加载。

## 备选说明
- 若后续也要发布 `bydwidget`，需先把 `bydrepo` 里的 `widget-tluc`/`animation` 一并上传到远程（其 POM 未声明 lottie 传递依赖），并显式 `api` lottie —— 本次不涉及。
