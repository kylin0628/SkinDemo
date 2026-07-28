# 依赖库升级与代码优化方案

## 一、当前状态摸底

### 现有版本
| 组件 | 当前版本 | 最新稳定版 |
|------|---------|-----------|
| Gradle | 6.1.1 | 8.5 |
| AGP | 4.0.1 | 8.2.0 |
| Kotlin | 1.3.72 | 1.9.22 |
| appcompat | 1.2.0 | 1.6.1 |
| core-ktx | 1.3.1 | 1.12.0 |
| constraintlayout(skinlibrary) | 2.0.0-rc1 | 2.1.4 |
| constraintlayout(app) | 1.1.3 | 2.1.4 |
| navigation-* | 2.3.0 | 2.7.7 |
| gson | 2.8.6 | 2.10.1 |
| compileSdk | 29 | 34 |
| targetSdk | 29 | 34 |
| minSdk(skinlibrary) | 21 | → 23 (对齐app) |
| minSdk(app) | 23 | 23 |

### 已发现的兼容性问题

1. **`kotlin-android-extensions` 插件**：Kotlin 1.4.20 起已废弃，1.9.x 完全移除。当前代码未使用 `kotlinx.android.synthetic`，可直接删除此插件。
2. **`AssetManager::class.java.newInstance()`**：Java 9 + Kotlin 1.5+ 已标记为 deprecated，需改为 `AssetManager::class.java.getDeclaredConstructor().newInstance()`
3. **`TextUtils.isEmpty()`**：Android 平台级 API 未硬性废弃，但可用 Kotlin 原生 `isNullOrEmpty()` 替代，减少对 Android 框架类的依赖。
4. **`@SuppressLint("UseCompatLoadingForDrawables")` + `getDrawable(id, null)`**：应改用 `ContextCompat.getDrawable(context, id)` 以消除 lint 警告。
5. **`@RequiresApi(Build.VERSION_CODES.M)`**：skinlibrary minSdk=21，所以此注解有实际意义。但若将 minSdk 升至 23（与 app 对齐），则可移除全部 `@RequiresApi(M)` 注解。
6. **重复类**：`beans/` 和 `model/` 目录下有相同的 `AttrsBean` 和 `SkinCache` 类，`model/` 是新版本（带 `by lazy`），应删除 `beans/` 下的旧文件。
7. **无用依赖**：`gson`、`navigation-fragment-ktx`、`navigation-ui-ktx` 在代码中无任何引用，可移除。
8. **`javaClass`**：在 Kotlin 1.9 中仍可用，但 `this::class.java` 更符合 Kotlin 惯用写法。影响面大（日志 TAG + 多处），不强制替换，但建议统一风格。

## 二、升级方案

### 目标版本
| 组件 | 目标版本 |
|------|---------|
| Gradle wrapper | 6.1.1 → **7.5**（AGP 7.4+ 要求） |
| AGP | 4.0.1 → **7.4.2**（稳定且兼容 Gradle 7.x） |
| Kotlin | 1.3.72 → **1.8.22**（与 AGP 7.4.2 最佳搭配） |
| compileSdk | 29 → 34 |
| targetSdk | 29 → 34 |
| minSdk(skinlibrary) | 21 → 23 |
| appcompat | 1.2.0 → 1.6.1 |
| core-ktx | 1.3.1 → 1.12.0 |
| constraintlayout | → 2.1.4 |
| material | → 1.9.0（material button 需要） |

**选择 AGP 7.4 + Kotlin 1.8 而非 AGP 8.x 的原因**：AGP 8.x 要求 Gradle 8.0+、需要 namespace 迁移（移除 AndroidManifest 中的 package）、强制非传递 R 类。升级步子太大风险高。AGP 7.4.2 是最成熟的 7.x 版本。

### namespace 迁移
AGP 7.0+ 支持在 build.gradle 中声明 `namespace`，替代 AndroidManifest 中的 `package`。这是 AGP 8.x 的强制要求，提前适配。

## 三、代码兼容性修复清单

### 3.1 全局删除 `kotlin-android-extensions` 插件
- `app/build.gradle`：删除第 3 行
- `skinlibrary/build.gradle`：删除第 3 行

### 3.2 SkinManager.kt（核心改动）

| 位置 | 旧代码 | 新代码 |
|------|--------|--------|
| 行 12 | `import android.text.TextUtils` | 删除 |
| 行 96 | `TextUtils.isEmpty(skinPath)` | `skinPath.isNullOrEmpty()` |
| 行 117 | `AssetManager::class.java.newInstance()` | `AssetManager::class.java.getDeclaredConstructor().newInstance()` |
| 行 146 | `TextUtils.isEmpty(skinPackageName)` | `skinPackageName.isNullOrEmpty()` |
| 行 188 | `@RequiresApi(Build.VERSION_CODES.M)` + `getColor(id, null)` | 改用 `ContextCompat.getColor()`（同时解决兼容问题） |
| 行 199 | `getColorStateList(id, null)` | 改用 `ContextCompat.getColorStateList()` |
| 行 211-218 | `getDrawable(id, null)` + `@SuppressLint` | 改用 `ContextCompat.getDrawable()` |
| 行 246 | `TextUtils.isEmpty(skinTypefacePath)` | `skinTypefacePath.isNullOrEmpty()` |

### 3.3 删除 `beans/` 下重复文件
- `beans/AttrsBean.kt` → 删除（`model/AttrsBean.kt` 新版本使用 `by lazy`）
- `beans/SkinCache.kt` → 删除（`model/SkinCache.kt` 新版本参数更精简）

### 3.4 移除无用依赖
- `skinlibrary/build.gradle`：删除 gson、navigation-*、material（无实际用法，app 模块有 material button 的引用但 skinlibrary 自身需要）
- 检查后再确认

### 3.5 SkinActivity 中 `instance!!` 安全化
- 行 153：`SkinManager.instance!!.getColor()` → 添加 `?: return` 空安全保护

## 四、文件变更预估

| 文件 | 操作 | 说明 |
|------|------|------|
| `gradle/wrapper/gradle-wrapper.properties` | 修改 | gradle 6.1.1 → 7.5 |
| `build.gradle`（根） | 修改 | AGP + Kotlin 版本、repositories 更新 |
| `skinlibrary/build.gradle` | 修改 | 升级依赖、删除无用依赖、namespace、删除 kotlin-android-extensions |
| `app/build.gradle` | 修改 | 升级依赖、namespace、删除 kotlin-android-extensions |
| `skinlibrary/SkinManager.kt` | 修改 | 6 处 API 兼容性修复 |
| `skinlibrary/beans/AttrsBean.kt` | 删除 | 重复文件 |
| `skinlibrary/beans/SkinCache.kt` | 删除 | 重复文件 |
| `skinlibrary/base/SkinActivity.kt` | 修改 | 1 处空安全修复 |

## 五、步骤执行顺序

1. 升级 Gradle wrapper → 7.5
2. 升级 AGP + Kotlin 版本（根 build.gradle）
3. 添加 namespace，删除 kotlin-android-extensions（两个子模块 build.gradle）
4. 升级所有依赖库版本
5. 删除重复文件（beans/）
6. 修复 SkinManager 中的 deprecated API
7. 清理无用 import 和注解
