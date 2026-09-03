# 比亚迪控件接入全局主题切换

## 目标

让 `BydWidgetDemoActivity` 里的比亚迪官方控件（`com.byd.widget.*`）也能跟随全局「动态皮肤包 / 默认主题」切换，与宿主换肤体验一致。

## 现状与结论

- 比亚迪控件（AAR 预编译）取色走 widget-tluc 内置 token，**不经过 `SkinManager`**；其 `onConfigurationChanged` 只监听 `uiMode`（暗黑模式），对动态皮肤包无感知。
- 布局 `activity_byd_widget_demo.xml` 里颜色全是**字面量**（`#111111` 等），无资源 ID，皮肤包无从按名映射。
- `SkinManager` 是全局单例，`loadSkin()` → `notifySkinChange()` 广播，`skinVersion++`。
- 皮肤包按「同名资源」映射：`SkinManager.getColor(resId)` 反查名称 → 皮肤包 `getIdentifier` 同名资源。
- 已验证 `skindemo.skin` 与 `skinpackage-release-unsigned.apk` 逐字节一致（仅文件后缀不同），重建 = 编译 skinpackage release 后复制产物。

## 关键约束（已确认）

比亚迪控件可用的公开 setter（`setTextColor`/`setCardBackgroundColor`/`backgroundTintList`/`*TintList`）**不碰 `setBackground`**，因此**不会破坏**按压/缩放/白蒙层动画。

## 实施步骤

### 1. skinlibrary 新增「桥颜色」（换肤颜色契约）
`skinlibrary/src/main/res/values/colors.xml` 增加 4 个语义颜色（默认浅色值）：
- `skin_bridge_text_primary` = `#E6000000`
- `skin_bridge_text_secondary` = `#88888888`
- `skin_bridge_card_bg` = `#FFFFFFFF`
- `skin_bridge_primary` = `#FF3388FF`

放 skinlibrary（而非 app），因 bydwidget 需引用这些 ID，且 app 依赖 bydwidget、不能反向。

### 2. bydwidget 依赖 skinlibrary
`bydwidget/build.gradle` 加 `implementation project(':skinlibrary')`（无循环依赖）。

### 3. 新增 `BydThemeBridge.kt`（bydwidget 模块）
`object BydThemeBridge { fun apply(root: View) }`：遍历 View 树，按控件类型用 `SkinManager` 取色刷：
| 控件 | setter（动画安全） | 颜色 |
|---|---|---|
| `BydTextView` | `setTextColor` | `skin_bridge_text_primary` |
| `BydButton` | `setTextColor` + `backgroundTintList` | 白字 / `skin_bridge_primary` |
| `BydEditText` | `setTextColor` + `setHintTextColor` | `text_primary` / `text_secondary` |
| `BydCardView` | `setCardBackgroundColor` | `skin_bridge_card_bg` |
| `BydProgressBar` | `setProgressTintList` | `skin_bridge_primary` |
| `BydSeekBar` | `setProgressTintList` + `setThumbTintList` | `skin_bridge_primary` |
| `BydSwitch` | `setThumbTintList` + `setTrackTintList` + `setTextColor` | `skin_bridge_primary` / `text_primary` |

默认皮肤回退 `ContextCompat` 取宿主同名色；非默认走 `SkinManager.getColor`。

### 4. 布局改资源 ID
`activity_byd_widget_demo.xml` 字面量色改 `@color/skin_bridge_*`（默认值），切肤时桥重刷。

### 5. `BydWidgetDemoActivity` 接入全局切肤
- `onCreate` 后 `BydThemeBridge.apply(root)` 刷一次。
- `SkinManager.addSkinChangeListener { BydThemeBridge.apply(root) }`（`onDestroy` 移除）。
- 装 `ThemeSwitcher` FAB（见第 6 点放宽）。

### 6. `ThemeSwitcher` 解耦 `SkinActivity`
- `installFab`/`show` 参数从 `SkinActivity` 放宽到 `Activity`；新增 `findActivity(context)` 递归解包。
- 切肤动作做宿主判断：
  - `SkinActivity` → 走现有 `skinDynamic()/defaultSkin()`。
  - 普通 Activity（BYD 页）→ `SkinManager.loadSkin(path, colorId)`，靠 `BydThemeBridge` 监听器刷新。

### 7. 皮肤包补同名色 + 重建产物
`skinpackage/src/main/res/values/colors.xml` 补同名色（暗色/主题橙）：
- `skin_bridge_text_primary` = `#FFFFFFFF`
- `skin_bridge_text_secondary` = `#B3FFFFFF`
- `skin_bridge_card_bg` = `#1E1E1E`
- `skin_bridge_primary` = `#FF6D00`

重建：编译 `skinpackage` release → 复制 `skinpackage-release-unsigned.apk` → `skinlibrary/src/main/assets/skin/skindemo.skin`。

### 8. `SkinActivity.onResume` 兜底
`override fun onResume()` 调 `applyCurrentSkin()`，保证从 BYD 页切肤后返回主页面颜色一致。

## 验证
1. `./gradlew :app:assembleDebug` 编译通过。
2. 进入 BYD 演示页 → FAB 切「动态主题」→ 比亚迪控件文字/卡片/tint 跟随变暗/变橙。
3. 切回「默认主题」→ 恢复浅色。
4. 切肤后返回主页面 → 主页面 Skinnable 控件颜色一致（onResume 兜底）。
5. 比亚迪按压/缩放/白蒙层动画仍正常（setter 不碰 background）。
