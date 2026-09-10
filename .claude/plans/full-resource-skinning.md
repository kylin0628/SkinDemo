# 四类资源换肤：原生页 text + textSize 换肤能力补齐 + 全量资源抽取与主题包镜像

## 目标

切主题（默认 ↔ 动态皮肤包）时，**原生页 + Compose 页**的 **图片 / dimens / string / color** 四类资源同时变化，且主题包资源与宿主默认资源**值有明显区分**。

## 关键事实（已调研确认）

### 现状
- **color / drawable**：原生换肤已支持。`attrs.xml` styleable 有 `background/textColor/textColorHint/src/各种 tint`，`Skinnable*.skinnableView()` 按名映射重刷。
- **string / dimen**：原生换肤**不支持**。`attrs.xml` 里**没有** `android:text`/`android:textSize`，`SkinnableTextView.skinnableView()` 也不重刷 `text`/`textSize`。所以布局即使写 `@string`/`@dimen`，切肤时原生文字/字号也不变。
- **Compose 页四类已全部支持**：`colorResource/painterResource/dimensionResource/stringResource` 走 `SkinnableResources` 的 `getColor/getDrawable/getDimension/getString`，每次重组重读，切肤即变。

### 关键机制（决定实现方式）
- `AttrsBean.saveViewResource` 用 `typedArray.getResourceId(i, -1)` 记录资源 ID：**`@string`/`@dimen` 引用返回真实 ID，字面量（`"① 主题切换"`、`"16sp"`）返回 -1**。
- 因此 skinnableView 里 `if (resId > 0)` 天然**只对显式引用资源**的 text/textSize 换肤，字面量自动跳过——无覆盖字面量文本的风险。

### 边界（必须在方案中守住）
1. **padding/margin 的 dp 不换肤**：切肤不重刷 LayoutParams，即使抽成 `@dimen` 也不变。dimen 换肤落地为 **textSize（sp）** + Compose 已有 `skin_demo_gap`。
2. **运行时 `setText` 的动态文本不抽取**：如 `SkinTestDialogFragment` 的 `statusView.text = if(isDefault)…`、`ThemeSwitcher` 的状态文案。这类控件的 XML text 保持字面量，避免切肤 `skinnableView` 用资源值覆盖运行时赋值。
3. **EditText / TextClock 的 text 覆盖语义**：demo 中无 XML `android:text`（只有 hint / format24Hour），安全。框架能力虽加，但仅在 `@string` 显式声明时触发。
4. **TabLayout 的 tab 文字、TextInputLayout 的 hint** 不在本次 text 换肤范围（换肤路径不同），保持现状。

## 实施步骤

### 1. 框架扩展：text + textSize 换肤（皮肤库 skinlibrary）

**1a. `SkinManager` 新增统一换肤入口**（避免 13 个控件重复逻辑）：

```kotlin
fun applyTextSkin(view: TextView, textRes: Int, textSizeRes: Int) {
    if (textRes > 0) view.text = getString(textRes)                       // 按名映射皮肤包
    if (textSizeRes > 0) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimension(textSizeRes))
}
```
（需 import `android.widget.TextView`、`android.util.TypedValue`。`getDimension` 已返回 px，配 `COMPLEX_UNIT_PX` 单位正确。）

**1b. `attrs.xml`**：给 13 个 TextView 系 styleable 追加 `<attr name="android:text" />` + `<attr name="android:textSize" />`：

- SkinnableTextView / Button / EditText / AutoCompleteTextView / MultiAutoCompleteTextView / CheckBox / RadioButton / CheckedTextView / SwitchCompat / ToggleButton / Chip / TextClock / TextInputEditText

**1c. 13 个 `Skinnable*.kt` 的 `skinnableView()`**：末尾追加

```kotlin
// text + textSize（dimen）：仅 @string/@dimen 引用换肤，字面量 getResourceId=-1 跳过
val textKey = R.styleable.SkinnableXxx[R.styleable.SkinnableXxx_android_text]
val textSizeKey = R.styleable.SkinnableXxx[R.styleable.SkinnableXxx_android_textSize]
manager.applyTextSkin(this, attrsBean.getViewResource(textKey), attrsBean.getViewResource(textSizeKey))
```

### 2. 资源抽取（宿主 app）

**2a. `values/strings.xml`**：把 8 个布局里的**静态**硬编码 `android:text="…"` 抽成 `<string>`，按语义命名（section 标题 `section_*`、按钮 `btn_*`、控件标签 `label_*`、描述 `desc_*`）。运行时 `setText` 的控件（`tv_skin_status`、`tv_theme_switcher_status` 等）**不抽**。

**2b. `values/dimens.xml`**：把静态硬编码 `android:textSize="Nsp"` 抽成 `<dimen name="text_size_*">Nsp</dimen>`（如 `text_size_title=18sp`、`text_size_body=16sp`、`text_size_hint=13sp`、`text_size_large=20sp`、`text_size_xlarge=22sp`）。布局改 `android:textSize="@dimen/text_size_*"`。

**2c. 布局替换**：`activity_main.xml` + 7 个 dialog/popup 布局中，`android:text="字面量"` → `@string/xxx`，`android:textSize="Nsp"` → `@dimen/text_size_*`。**`android:textColor` 已是 `@color` 引用，不动**。

### 3. 主题包镜像（skinpackage，值有区分）

**3a. `skinpackage/.../values/strings.xml`**：为每个抽出的 string 提供**同名但内容不同**的皮肤包版（统一加「· 动态皮肤」后缀或改写措辞），确保切肤肉眼可见文案变化。

**3b. `skinpackage/.../values/dimens.xml`**：为每个 `text_size_*` 提供**同名不同值**（如 `text_size_title=20sp`、`text_size_body=18sp`、`text_size_hint=15sp`），使字号随切肤明显变化。

> color（21 个）、drawable（`ic_skin_demo` 蓝→橙）此前已镜像完成，本轮不动。

### 4. 验证

- `./gradlew :skinpackage:assembleRelease` 构建通过。
- `aapt2 dump resources` 确认皮肤包含全部 string/dimen 同名资源。
- 运行时切肤：原生页 section 标题/按钮/控件标签文字变化、字号变化，配合 color/drawable 同步；Compose 页四类仍同步。

## 交付物清单

| 层 | 文件 | 改动 |
|---|---|---|
| 框架 | `SkinManager.kt` | + `applyTextSkin` |
| 框架 | `attrs.xml` | 13 个 styleable + text/textSize |
| 框架 | 13 个 `Skinnable*.kt` | skinnableView + text/textSize 重刷 |
| 宿主 | `app/values/strings.xml` | + ~40 个静态 string |
| 宿主 | `app/values/dimens.xml` | + text_size_* 系列 |
| 宿主 | 8 个布局 | text/textSize → @string/@dimen |
| 主题包 | `skinpackage/values/strings.xml` | 同名不同值 |
| 主题包 | `skinpackage/values/dimens.xml` | 同名不同值 |
