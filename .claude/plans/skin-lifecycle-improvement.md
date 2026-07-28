# 皮肤切换生命周期感知改进方案（最终版）

## 现状分析

### 当前问题
1. **无生命周期感知**：每个 Activity 必须在 `onCreate` 中手动调用 `skinDynamic()` 或 `defaultSkin()` 来恢复皮肤状态
2. **状态管理分散**：皮肤状态通过 `SkinManager.isDefaultSkin`（内存）和 `SharedPreferences.get("currentSkin")`（持久化）两处管理，容易不一致
3. **代码重复**：每个新 Activity 都需要复制粘贴换肤逻辑（如 `MainActivity.onCreate` 中的 `PreferencesUtils.getString` 检查）
4. **无自动恢复**：如果新增 Activity，必须手动添加换肤逻辑

### 当前架构流程
```
MainActivity.onCreate
  ├── 读 SharedPreferences 判断当前皮肤
  ├── 是"skindemo" → skinDynamic(skinPath, themeColorId)
  │     ├── SkinManager.loaderSkinResources(skinPath)
  │     ├── StatusBar/Navigation/ActionBar 换肤
  │     └── applyViews(decorView) → 递归遍历所有 View.skinnableView()
  └── 否 → defaultSkin(themeColorId)
```

## 改进方案

### 方案概述
**核心思路：将皮肤状态管理集中到 `SkinManager`，通过在 `SkinActivity.onPostCreate()` / `onResume()` 中自动感知生命周期，让子 Activity 无需手动恢复皮肤状态。**

### 具体改动

#### 1. SkinManager 增强（skinlibrary/core/SkinManager.kt）

**新增字段：**
- `currentSkinPath: String?` — 追踪当前加载的皮肤包路径（null = 默认皮肤），private set
- `currentThemeColorId: Int` — 当前主题色资源 ID，private set

**新增方法：**
- `loadSkin(skinPath: String?, themeColorId: Int)` — 统一加载皮肤入口，内部更新状态并调用 loaderSkinResources()

#### 2. SkinActivity 内置生命周期感知（skinlibrary/base/SkinActivity.kt）

**新增字段：**
- `isFirstCreate: Boolean` — 标记是否首次创建

**重写生命周期方法：**
- `onPostCreate()` — 在子类 setContentView() 之后自动应用当前皮肤（视图树已建立）
- `onResume()` — 非首次创建时（从后台返回），重新检查并应用皮肤状态

**改动：**
- `skinDynamic(skinPath, themeColorId)` 中调用 `SkinManager.loadSkin()` 代替直接调用 `loaderSkinResources()`
- 新增 `applyCurrentSkin()` 方法 — 从 SkinManager 获取当前状态并自动应用

#### 3. SkinApp 状态恢复（app/SkinApp.kt）

**新增方法：**
- `restoreSkinState()` — 从 SharedPreferences 恢复上次皮肤状态，注入 SkinManager

**改动：**
- 在 `onCreate` 中先调用 `restoreSkinState()` 再初始化资源
- 不再注册 ActivityLifecycleCallbacks（生命周期感知内置于 SkinActivity）

#### 4. MainActivity 简化（app/MainActivity.kt）

**改动：**
- 移除 `onCreate` 中的皮肤状态手动判断
- 移除按钮点击中 `PreferencesUtils` 的重复读取，改为用 `SkinManager.currentSkinPath` 判断

### 流程图（改进后）

```
App 启动
  └── SkinApp.onCreate
        ├── SkinManager.init(this)
        ├── restoreSkinState()  ← 从 SharedPreferences 恢复状态到 SkinManager
        └── AssetsUtils.doCopy()

Activity 启动
  └── SkinActivity.onCreate
        ├── 设置 Factory2（拦截控件创建）
        └── super.onCreate (Fragment 恢复等)
  └── SkinActivity.onPostCreate  ← setContentView() 之后
        └── if (openChangeSkin())
              └── applyCurrentSkin()
                    ├── 从 SkinManager 获取 currentSkinPath + currentThemeColorId
                    ├── skinDynamic(skinPath, themeColorId)
                    │     ├── SkinManager.loadSkin(skinPath, themeColorId)
                    │     ├── StatusBar/Navigation/ActionBar 换肤
                    │     └── applyViews(decorView)

Activity 从后台恢复
  └── SkinActivity.onResume
        └── if (!isFirstCreate && openChangeSkin())
              └── applyCurrentSkin()

用户点击换肤按钮
  └── MainActivity.skinDynamic(view)
        └── SkinActivity.skinDynamic(skinPath, themeColorId)
              ├── SkinManager.loadSkin(skinPath, themeColorId) ← 记录到内存
              ├── StatusBar/Navigation/ActionBar 换肤
              └── applyViews(decorView)
```

### 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `skinlibrary/.../SkinManager.kt` | 修改 | 新增 `currentSkinPath`、`currentThemeColorId`、`loadSkin()` 方法 |
| `skinlibrary/.../base/SkinActivity.kt` | 修改 | 新增 `onPostCreate`/`onResume` 自动换肤逻辑、`applyCurrentSkin()` |
| `app/.../SkinApp.kt` | 修改 | 新增 `restoreSkinState()` 恢复初始状态 |
| `app/.../MainActivity.kt` | 修改 | 移除手动皮肤状态恢复逻辑 |

### 设计优势
1. **零侵入**：新增 Activity 只需继承 `SkinActivity`，无需写任何换肤代码
2. **状态统一**：皮肤状态只存在 `SkinManager.currentSkinPath`，SharedPreferences 仅作为冷启动恢复的辅助存储
3. **自动恢复**：onPostCreate 处理首次创建、onResume 处理从后台返回，生命周期全覆盖
4. **向后兼容**：现有 API 保持不变，不影响现有功能
5. **无需 LifecycleCallbacks**：生命周期感知内置于 SkinActivity 本身，减少额外的类依赖
