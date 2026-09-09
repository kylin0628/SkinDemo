# BYD 控件全量展示 + 完整主题切换

## 目标

`BydWidgetDemoActivity` 从「7 个控件」扩展到**完整展示比亚迪控件库**，其中**有公开颜色 setter 的控件接入动态皮肤**（切肤/切深浅色跟随），**内部 token 取色的复合控件做展示**（跟随 BYD 内置暗黑/浅色，不跟随动态主题色）。

## 关键事实（已调研确认）

- BYD AAR（`widget-tluc-1.10.4`）控件包 `com.byd.widget.*`，多数有 `(Context, AttributeSet)` 构造器，可被 XML 反射 inflate。
- 控件分两类：
  - **可换肤**（公开 setter，动画安全）：`BydTextView/BydButton/BydEditText/BydCardView/BydProgressBar/BydSeekBar/BydSwitch`（已接入）+ 新增 `BydCheckBox/BydRadioButton/BydImageView/BydDivider/BydSlideBar/BydTextInputLayout`。
  - **展示型**（内部 `bydPvt*` token 取色，仅跟 uiMode 暗黑/浅色）：`BydTitleBar/BydSearchView/BydNumberPicker/BydDatePicker/BydTimePicker/BydListItem/BydSwitchListItem/BydTabLayout/BydSideBar/BydAlertDialog/BydPopupMenu`。
- `SkinManager` 按「同名资源」映射：`getColor(resId)` 反查 entryName → 皮肤包 `getIdentifier` 同名资源。缺名回退宿主。
- 换肤语义色 4 个已就位（bydwidget 宿主默认 + skinpackage 暗色/主题橙均已有）：
  - `skin_bridge_text_primary` / `skin_bridge_text_secondary` / `skin_bridge_card_bg` / `skin_bridge_primary`
- 现有 `SkinnableByd*` 模式：`AttrsBean` + `withStyledAttributes` 记录 XML 资源 ID，`skinnableView()` 经 `SkinManager.getColor/getColorStateList` 按名映射刷新，**不碰 setBackground**（保动画）。
- 工厂接入：`BydSkinActivity` 用 `registerSkinnableViews(List<SkinnableViewBinder>)` 把「控件类 + 标签名集合」注入主题库责任链；未命中的标签回落 `CustomAppCompatViewInflater` / AppCompat 反射 inflate。

## 实施步骤

### 1. `bydwidget/.../views/SkinnableBydViews.kt` 新增 6 个可换肤包装类
沿用现有 `AttrsBean` 模式，每个类：`init` 用 `withStyledAttributes` 记录资源 ID，`skinnableView()` 按名取色刷新，`onAttachedToWindow` 调 `applySkinIfChanged`。

| 类 | 继承 | 换肤属性 | setter |
|---|---|---|---|
| `SkinnableBydCheckBox` | `BydCheckBox` | `android:textColor`、`android:buttonTint` | `setTextColor`/`setButtonTintList` |
| `SkinnableBydRadioButton` | `BydRadioButton` | `android:textColor`、`android:buttonTint` | `setTextColor`/`setButtonTintList` |
| `SkinnableBydImageView` | `BydImageView` | `android:src`、`android:background` | `setImageDrawable`/`setBackground` |
| `SkinnableBydDivider` | `BydDivider` | `android:background`(color) | `setBackgroundColor` |
| `SkinnableBydSlideBar` | `BydSlideBar` | `android:progressTint`、`android:thumbTint` | `setProgressTintList`/`setThumbTintList` |
| `SkinnableBydTextInputLayout` | `BydTextInputLayout` | `app:hintTextColor`、`app:boxBackgroundColor` | `setHintTextColor`/`setBoxBackgroundColor` |

> 注意：`BydDivider extends android.view.View`、`BydSlideBar extends AppCompatSeekBar`、`BydTextInputLayout extends LinearLayout`（box bg 用 `setBoxBackgroundColor(int)`）。`BydImageView extends LottieAnimationView`，`src` 走 `getDrawableOrMipMap` 同名 drawable 换肤。

### 2. `bydwidget/.../res/values/attrs.xml` 补 declare-styleable
为上述 6 个类新增 styleable 块（`SkinnableBydCheckBox` / `SkinnableBydRadioButton` / `SkinnableBydImageView` / `SkinnableBydDivider` / `SkinnableBydSlideBar` / `SkinnableBydTextInputLayout`），声明各自属性引用。`hintTextColor`/`boxBackgroundColor` 为自定义 app 属性需 `format="color|reference"`。

### 3. `bydwidget/.../BydSkinActivity.kt` 补绑定
在 `bydSkinnableViewBinders` 列表追加：
- `setOf(CHECK_BOX)` → `::SkinnableBydCheckBox`
- `setOf(RADIO_BUTTON)` → `::SkinnableBydRadioButton`
- `setOf(IMAGE_VIEW)` → `::SkinnableBydImageView`
- `setOf(FQCN "com.byd.widget.BydDivider")` → `::SkinnableBydDivider`
- `setOf(FQCN "com.byd.widget.BydSlideBar")` → `::SkinnableBydSlideBar`
- `setOf(FQCN "com.byd.widget.BydTextInputLayout")` → `::SkinnableBydTextInputLayout`

新增本地常量 `BYD_DIVIDER`/`BYD_SLIDE_BAR`/`BYD_TEXT_INPUT_LAYOUT`（比亚迪 FQCN 属业务层，不放通用 `SystemViewName`），避免魔法字符串。

### 4. `bydwidget/.../res/layout/activity_byd_widget_demo.xml` 全量展示
按分区扩展（沿用标准标签 + BYD FQCN 标签）：

- **基础文本/按钮**：TextView / Button / EditText / CardView（已有）
- **选择**：`<CheckBox>`、`<RadioGroup>`+`<RadioButton>`（`buttonTint`/`textColor` 指向 `skin_bridge_*`）
- **进度**：ProgressBar / SeekBar（已有）+ `<com.byd.widget.BydSlideBar>`（`progressTint`/`thumbTint`）
- **开关**：Switch（已有）
- **图片/分割**：`<ImageView>`（`src`+`background`）、`<com.byd.widget.BydDivider>`（`background`）
- **输入**：`<com.byd.widget.BydTextInputLayout>`（内嵌 EditText，`hintTextColor`/`boxBackgroundColor`）
- **展示型（不换肤，FQCN 反射 inflate）**：`BydTitleBar`、`BydSearchView`、`BydNumberPicker`、`BydDatePicker`、`BydTimePicker`、`BydListItem`、`BydSwitchListItem`、`BydTabLayout`、`BydSideBar` —— 各分区标题注明「跟随 BYD 内置暗黑/浅色，不跟随动态主题色」
- **弹框入口**：两个 `<Button>`，onClick 分别弹 `BydAlertDialog` 与 `BydPopupMenu`（在 `BydWidgetDemoActivity` 里实现）

### 5. `bydwidget/.../BydWidgetDemoActivity.kt` 补弹框演示
`onClick` 方法：`showBydAlertDialog()`（`BydAlertBuilder` 造简单告警框）、`showBydPopupMenu()`（`BydPopupMenu` 造简单菜单）；标题/文案用 `skin_bridge_*` 语义色。展示型控件的最小初始化（`BydListItem.setTitle/setSummary`、`BydTitleBar.setTitle`）在 onCreate 内 `findViewById` 后调用。

### 6. 验证
- `./gradlew :bydwidget:compileDebugKotlin :app:assembleDebug` 编译通过。
- 运行进 BYD 演示页：
  1. 所有控件正常渲染、无崩溃。
  2. FAB 切「动态主题」→ 可换肤控件文字/勾选/tint/卡片/分割线/输入框跟随变暗变橙。
  3. 切「默认主题」→ 恢复浅色。
  4. 展示型控件在系统暗黑模式切换下跟随 BYD 内置深浅色。
  5. 按压/缩放/白蒙层/过滚动动画不破坏。

## 不做（本计划范围外）
- 不对 DatePicker/TimePicker/NumberPicker/SearchView/PopupMenu/AlertDialog/SideBar 做深度动态换肤（需覆盖 AAR 内部 token，风险高，已按用户确认保留为「展示型」）。
- 不新增皮肤包颜色：换肤仍复用现有 4 个 `skin_bridge_*`（skinpackage 已有同名色，无需重建 skindemo.skin）。
