# skinlibrary 主题库使用指南

运行时「皮肤包」换肤框架：宿主 App 通过加载一个独立的皮肤 APK（`skindemo.skin`），按资源**同名映射**替换颜色 / 图片 / 字符串 / 尺寸，实现「不重装、不重启」切换整套主题。

本文按 **完整 → 初步 → 高级 → 深入** 四层组织，逐层递进：

- [完整](#一完整主题库全貌)：架构、模块划分、数据流、核心概念。
- [初步](#二初步快速接入)：最小可用的接入步骤（继承基类 + XML 属性即可换肤）。
- [高级](#三高级进阶用法)：Compose、独立窗口、全局切换器、跟随系统深浅色。
- [深入](#四深入原理与扩展)：内部实现、第三方控件接入、新增可换肤资源、皮肤包构建、排查。

---

## 一、完整：主题库全貌

### 1.1 模块划分

| 模块 | 角色 | 说明 |
|---|---|---|
| `skinlibrary` | 换肤核心库 | 通用换肤能力，`namespace = com.kylin.skinlibrary`（基类在 `com.netease.skin.library.*`） |
| `skinpackage` | 皮肤包 | `applicationId = com.kylin.skinpackage`，编译成 APK 后改名 `.skin` 供宿主加载 |
| `app` | 宿主演示 | 继承 `SkinActivity` 的业务页 + 全局切换器 |
| `bydwidget` | 第三方控件接入示例 | 比亚迪控件（`com.byd.widget.*`）接入换肤链路的完整范式 |

### 1.2 核心数据流

```
Application.onCreate
  └─ SkinManager.init(app)                 // 1. 全局单例，最早初始化
  └─ SkinUiHost.applyTheme = { ... }        // 2. 宿主注册「应用一套主题」统一策略
  └─ AppCompatDelegate.setDefaultNightMode(FOLLOW_SYSTEM)  // 3. 跟随系统深浅色

Activity : SkinActivity
  ├─ onCreate → LayoutInflaterCompat.setFactory2(this)     // 拦截 XML inflate
  ├─ onCreateView(name, ...)                                // 标签名 → Skinnable* 子类
  │     └─ CustomAppCompatViewInflater.autoMatch()          //   （内置 50+ 原生控件映射）
  │     └─ registerSkinnableViews(binders)                  //   （第三方控件参数注册）
  ├─ init  → AttrsBean.saveViewResource(...)               // 记录 background/textColor 等属性资源 ID
  └─ onPostCreate → applyCurrentSkin() → skinDynamic(...)  // 首次自动换肤

切换主题
  └─ SkinManager.loadSkin(skinPath)
       ├─ loaderSkinResources()   // 反射 AssetManager.addAssetPath 挂载皮肤包
       ├─ skinVersion++           // 版本号自增，触发 Compose 重组 / 原生监听
       └─ notifySkinChange()      // 广播：监听器 + 已注册独立窗口 applySkin()

Skinnable*.skinnableView()
  └─ AttrsBean.getViewResource(styleable)   // 取 inflate 时记录的资源 ID
  └─ SkinManager.getColor/resolveSkinId   // 按名映射到皮肤包同名资源（缺名回退宿主）
```

### 1.3 三个核心概念

1. **同名映射**：宿主资源 ID 先反查 `getResourceEntryName`，再到皮肤包 `getIdentifier` 找同名资源。皮肤包缺同名资源时回退宿主，并打 `[Skin]` warn 日志 `皮肤包缺少同名资源 → type/name`。这是「某资源没变」的 #1 根因。
2. **`ViewsMatch` 契约**：所有可换肤控件实现 `ViewsMatch.skinnableView()`。切肤时框架递归遍历 View 树，对命中的控件逐个调用该方法。
3. **`skinVersion` 信号**：每切一次肤 `SkinManager.skinVersion++`。原生侧靠 `notifySkinChange()` 监听器，Compose 侧靠 `LocalSkinVersion`（`staticCompositionLocalOf`）触发局部重组。

### 1.4 四维资源适配能力

| 维度 | 映射方式 | 取值优先级 |
|---|---|---|
| 颜色 | `color` / `colorStateList` | 皮肤包同名 → 宿主 |
| 图片 | `drawable` / `mipmap` | 皮肤包同名 → 宿主 |
| 字符串 | `string` / `text`（locale 感知） | 皮肤包同名 → 宿主 |
| 尺寸 / 整数 / 布尔 | `dimen` / `integer` / `bool` | 皮肤包同名 → 宿主 |

---

## 二、初步：快速接入

> 目标：让一个 Activity 里的原生控件（TextView / Button / ImageView …）在切肤时跟随变色。

### 2.1 依赖

宿主模块（`app`）依赖换肤库：

```gradle
implementation project(':skinlibrary')
```

### 2.2 Application 初始化（最早时机）

```kotlin
class SkinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SkinManager.init(this)          // 1. 全局单例，必须在任何换肤操作前
        // 2. 拷贝皮肤包到外部存储（assets/skin/ → getExternalFilesDir）
        AssetsUtils.doCopy(this, "skin", "${getExternalFilesDir("skindemo")!!.absolutePath}")
        // 3. 恢复上次皮肤状态（可选）
        SkinManager.instance?.loadSkin(skinPathOrNull)
    }
}
```

### 2.3 Activity 继承 `SkinActivity`

```kotlin
class MyActivity : SkinActivity() {   // 替代 AppCompatActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)   // 正常写布局，无需额外代码
    }
}
```

### 2.4 XML 布局照常写，属性自动换肤

```xml
<TextView
    android:textColor="@color/text_primary"      <!-- 文字色随皮肤切换 -->
    android:background="@color/dialog_bg"        <!-- 背景随皮肤切换 -->
    android:text="示例文本" />

<ImageView
    android:src="@drawable/ic_skin_demo" />       <!-- 同名 drawable 换肤 -->

<ProgressBar
    android:progressTint="@color/main_style" />   <!-- 进度条着色随皮肤切换 -->
```

**原理**：`SkinActivity` 设置了 `LayoutInflater.Factory2`，把 XML 里的 `TextView` / `Button` / `ImageView` 等标签自动替换成对应的 `Skinnable*` 子类，构造时用 `AttrsBean` 记录这些属性的资源 ID，切肤时按名映射到皮肤包同名资源。

### 2.5 切换皮肤

```kotlin
// 切到动态皮肤包
SkinManager.instance?.loadSkin(skinPath)

// 切回默认皮肤（传 null）
SkinManager.instance?.loadSkin(null)
```

在 `SkinActivity` 内更推荐用完整链路（含状态栏/导航栏/ActionBar + 遍历刷新 + 已打开弹框）：

```kotlin
activity.skinDynamic(skinPath)  // 动态皮肤
activity.defaultSkin()          // 默认皮肤
```

> 无需单独传主题色：状态栏/导航栏/ActionBar 的主题色由库自动从 Activity 主题
> （`colorAccent` → `colorPrimary` → `statusBarColor`）解析，并按名映射到皮肤包同名资源。

### 2.6 皮肤包要提供同名资源

宿主（浅色）与皮肤包（深色）各自定义**同名**资源，切肤时按名替换：

```xml
<!-- app/src/main/res/values/colors.xml（宿主浅色） -->
<color name="bg_page">#FFEBEDF0</color>
<color name="text_primary">#E6000000</color>

<!-- skinpackage/src/main/res/values/colors.xml（皮肤包深色） -->
<color name="bg_page">#121212</color>
<color name="text_primary">#FFFFFFFF</color>
```

缺同名资源会回退宿主并打 warn 日志。

---

## 三、高级：进阶用法

### 3.1 资源门面（手动取色）

`SkinManager` 提供一套 getter，业务在非 XML 场景（代码动态设色）用它按当前皮肤取值：

```kotlin
val m = SkinManager.instance
m.getColor(R.color.text_primary)             // 颜色
m.getColorStateList(R.color.tab_selected)    // 颜色选择器
m.getDrawableOrMipMap(R.drawable.ic_skin)    // 图片
m.getString(R.string.title)                  // 字符串
m.getDimension(R.dimen.gap)                  // 尺寸
```

**代码动态设色直接用系统方法即可**：所有文本类 `Skinnable*` 控件（`SkinnableTextView` / `SkinnableButton` /
`SkinnableEditText` / `SkinnableCheckBox` / `SkinnableRadioButton` / `SkinnableSwitchCompat` /
`SkinnableAutoCompleteTextView` / `SkinnableToggleButton` / `SkinnableCheckedTextView` /
`SkinnableChip` / `SkinnableTextClock` / `SkinnableMultiAutoCompleteTextView` /
`SkinnableTextInputEditText`）重写了 `setTextColor(int)` / `setTextColor(ColorStateList)`，
`view.setTextColor(context.getColor(R.color.x))` 会反推资源 ID 回填，切肤时可自动重映射，业务无需感知主题库：

```kotlin
view.setTextColor(context.getColor(R.color.text_primary))          // 颜色（切肤自动跟随）
view.setTextColor(context.getColorStateList(R.color.tab_selected)) // 颜色选择器
```

`SkinnableTextInputLayout` 的 hint 文字同理，重写了 `setHintTextColor(ColorStateList)`，用系统方法设色即可跟随换肤。

> 局限：字面量色值（`setTextColor(0xFF000000)`）无对应 `getColor` 调用、校验失败，不纳入换肤。换肤依赖「`getColor(res)` 先于 `setTextColor` 同线程紧邻执行」这一调用顺序（按最近一次解析的资源 ID 反推，并校验解析结果等于传入色值），同值不同名的多个资源不会串色——每个 `setTextColor` 取回的是它自己那次 `getColor` 的资源。

### 3.2 Compose 换肤

Compose 页只需包一层 `SkinTheme`（内部是 `SkinComposeProvider`），之后**系统资源方法零改动**即可换肤：

```kotlin
setContent {
    SkinTheme {                                       // 订阅皮肤版本，提供重组信号
        MaterialTheme(colorScheme = skinColorScheme()) {  // 皮肤色映射到 ColorScheme
            Text("标题", color = colorResource(R.color.text_primary))
            Image(painter = painterResource(R.drawable.ic_skin_demo))
            Spacer(Modifier.height(dimensionResource(R.dimen.skin_demo_gap)))
        }
    }
}
```

**为什么 `colorResource` / `painterResource` 能直接换肤**：`SkinActivity.getResources()` 被覆盖，非默认皮肤时返回 `SkinnableResources`（按名映射到皮肤包），Compose 系统资源方法内部都走 `LocalContext.current.resources.getXxx(id)`，故自动生效。

**状态保持**：`SkinTheme` 用 `staticCompositionLocalOf` 提供 `LocalSkinVersion`，只在读取点触发**局部重组**，不用 `key(skinVersion)` 整树重建——切主题后输入框文本、开关勾选、滑块值**不丢**。

如需显式包装（非继承 `SkinActivity` 的纯 Compose 场景），用主题库原生 API：

```kotlin
val c = skinnedColor(R.color.text_primary)          // 颜色，皮肤切换时局部重组
val s = skinnedString(R.string.title)               // 字符串
val d = skinnedDimension(R.dimen.gap)               // 尺寸
val p = skinnedPainter(R.drawable.ic_skin_demo)     // 图片（绕过 painterResource 全局缓存）
```

### 3.3 独立窗口跟随换肤（Dialog / PopupWindow）

Dialog / PopupWindow 拥有独立 Window，不在 `SkinActivity.applyViews(decorView)` 覆盖范围内。显示后调一次 `registerWindow`，此后每次切肤自动遍历：

```kotlin
// Dialog 内
setContentView(root)
SkinManager.instance?.registerWindow(root)   // 关键：注册根视图
```

PopupWindow 同理注册其 `contentView`。注册用弱引用，视图回收自动失效，无需反注册。

### 3.4 全局主题切换器（可选 UI）

`ThemeSwitcher` 提供悬浮按钮 + 切换弹框，向**每个独立 Window** 注入入口（单页多层弹框都能切肤）。在 `SkinActivity.onPostCreate` 挂载：

```kotlin
override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    ThemeSwitcher.installFab(this)
}
```

第三方模块（如 `bydwidget`）想挂载切肤入口但不想反向依赖 `app`，走 `SkinUiHost` 钩子：

```kotlin
// 宿主 Application 注册一次
SkinUiHost.installThemeSwitcher = { activity -> ThemeSwitcher.installFab(activity) }

// 第三方页面调用
SkinUiHost.installThemeSwitcher?.invoke(this)
```

### 3.5 跟随系统深浅色 + 统一「应用一套主题」

核心钩子是 `SkinUiHost.applyTheme(activity, isDark, forceNightMode)`，把「app 内切换」与「跟随系统变化」收敛到**同一条实现**：

```kotlin
SkinUiHost.applyTheme = { activity, isDark, forceNightMode ->
    // 1) 同步夜间模式（BYD 等按 uiMode 取色的控件才跟随）
    AppCompatDelegate.setDefaultNightMode(
        when {
            !forceNightMode -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM  // 跟随系统
            isDark          -> AppCompatDelegate.MODE_NIGHT_YES           // 用户主动切深色
            else            -> AppCompatDelegate.MODE_NIGHT_NO            // 用户主动切浅色
        }
    )
    // 2) 换肤：深色→动态皮肤，浅色→默认皮肤
    if (isDark) activity.skinDynamic(skinPath)
    else        activity.defaultSkin()
}
```

两个调用入口：

| 入口 | 参数 | 语义 |
|---|---|---|
| `SkinActivity.onDarkModeChanged(isDark)` | `forceNightMode=false` | 跟随系统：重置 `FOLLOW_SYSTEM`，仅换肤，不污染全局默认 |
| 切肤入口（切换器/按钮） | `forceNightMode=true` | 用户主动：强制 `YES/NO`，让 uiMode 立即随主题切 |

**关键坑**：跟随系统时**不能**强制 `YES/NO`，否则会污染全局默认夜间模式，导致无 `configChanges="uiMode"` 的页面（如比亚迪演示页）重建时读到被强制的旧值、卡在错误深浅色。

---

## 四、深入：原理与扩展

### 4.1 换肤库内部实现

#### 皮肤包加载（反射挂载 AssetManager）

```kotlin
// SkinManager.loaderSkinResources()
val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
val addAssetPath = assetManager.javaClass.getDeclaredMethod("addAssetPath", String::class.java)
addAssetPath.isAccessible = true
addAssetPath.invoke(assetManager, skinPath)                       // 挂载皮肤包 APK
skinResources = Resources(assetManager, appResources.displayMetrics, appResources.configuration)
skinPackageName = packageManager.getPackageArchiveInfo(skinPath, ...)?.packageName
```

皮肤包加载结果缓存到 `SkinCache`，同一路径二次加载命中缓存。

#### 同名资源映射（含缓存）

```kotlin
// SkinManager.getSkinResourceIds()
val name = appResources.getResourceEntryName(resourceId)   // 反查名称
val type = appResources.getResourceTypeName(resourceId)
val ids  = skinResources.getIdentifier(name, type, skinPackageName)  // 皮肤包同名 ID
// ids == 0 表示皮肤包缺名 → 回退宿主
```

宿主→皮肤 ID 映射缓存到 `ConcurrentHashMap`，皮肤切换时清空。

#### 控件替换（Factory2 + AttrsBean）

- `SkinActivity.onCreate` 调 `LayoutInflaterCompat.setFactory2(layoutInflater, this)`。
- `onCreateView` 里 `CustomAppCompatViewInflater.autoMatch()` 按标签名 `when(name)` 构造 `Skinnable*` 子类。
- 每个 `Skinnable*` 构造时 `withStyledAttributes(...)` 把 `background` / `textColor` / `src` 等属性的**资源 ID** 存进 `AttrsBean`（`SparseIntArray`）。
- 切肤遍历时 `skinnableView()` 从 `AttrsBean` 取回资源 ID，经 `SkinManager` 按名映射后 `setBackground` / `setTextColor` 等刷新。

#### 切肤遍历与去重

```kotlin
// SkinActivity.applyViews(view)  —— 递归遍历 View 树
if (view is ViewsMatch) view.skinnableView()
if (view is ViewGroup) for (i in 0 until childCount) applyViews(getChildAt(i))
```

`SkinManager.applySkinIfChanged(view)` 用 `skinVersion` 做去重：RecyclerView 缓存/离屏复用的 item 不在 `applyViews` 遍历范围内，靠 `onAttachedToWindow` 兜底重刷，同一皮肤版本内重复 attach 跳过，降低滚动阶段无效 invalidate。

### 4.2 第三方控件接入换肤（比亚迪范式）

预编译 AAR 控件（`com.byd.widget.*`）取色走内部 token，不经过 `SkinManager`。要让它们走主题库，三步：

**① 继承第三方控件 + 实现 `ViewsMatch`**

```kotlin
class SkinnableBydTextView(context: Context, attrs: AttributeSet?) :
    BydTextView(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val resId = attrsBean.getViewResource(
            R.styleable.SkinnableBydTextView[R.styleable.SkinnableBydTextView_android_textColor]
        )
        if (resId > 0) setTextColor(manager.getColorStateList(resId))
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydTextView, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydTextView)
        }
    }
}
```

**② 声明换肤语义属性**（第三方模块自己的 `res/values/attrs.xml`）

```xml
<declare-styleable name="SkinnableBydTextView">
    <attr name="android:textColor" />
</declare-styleable>
```

**③ 以绑定列表注册**（继承 `SkinActivity` 的基类里）

```kotlin
abstract class BydSkinActivity : SkinActivity() {
    private val binders = listOf(
        SkinnableViewBinder(setOf(SystemViewName.TEXT_VIEW, SystemViewName.ANDROID_TEXT_VIEW), ::SkinnableBydTextView),
        // ...
    )
    init { registerSkinnableViews(binders) }
}
```

要点：
- 颜色契约**下沉到 XML**：业务在布局里声明 `android:textColor` / `android:progressTint` 等，`SkinnableByd*` 只记录资源 ID 不改色，切肤时按名映射。新增语义色只需改 XML，不改控件类。
- 复合控件（如 `BydDivider`）无对应系统标签，用 **FQCN** 匹配：`"com.byd.widget.BydDivider"`。
- 换肤只改 `setTextColor` / `setCardBackgroundColor` / `*TintList`，**不碰 `setBackground`**，保留第三方按压/缩放/回弹动画。

### 4.3 新增一种「可换肤资源/属性」标准步骤

1. `SkinManager` 加 getter（复用 `getSkinResourceIds` + `useHost` 模式）。
2. 原生侧：`skinlibrary/res/values/attrs.xml` 加 `declare-styleable` 属性 + 对应 `Skinnable*.skinnableView()` 分支。
3. Compose 侧：`SkinnableResources` 加 override（若 Compose API 走 `LocalContext.current.resources`）。
4. 皮肤包提供同名资源。
5. 编译 `:skinlibrary :app`。

### 4.4 新增一种「可换肤控件类型」标准步骤

1. `SystemViewName` 加标签名常量（标准控件用系统名，专属控件用 FQCN）。
2. 新建 `SkinnableXxx`（继承对应控件 + `ViewsMatch`）。
3. `CustomAppCompatViewInflater.autoMatch()` 的 `when(name)` 加映射分支（或走 `registerSkinnableViews` 参数注册，无需改内置映射）。
4. `attrs.xml` 加 `declare-styleable` 声明要换肤的属性。

### 4.5 Factory 责任链契约（多库兼用）

framework 的 `LayoutInflater.setFactory2` **只能设一次**（第二次抛 `IllegalStateException`），故多库并存必须走责任链：

1. 第三方库**不自行调 `LayoutInflaterCompat.setFactory2`**，改调 `LayoutFactoryRegistry.register(factory)`。
2. 每个 factory 对不关心的 View **返回 `null` 放行**（否则吃掉链，后面的库收不到 View）。
3. 同类控件「先注册者优先」，后来者对该类控件静默失效。
4. 第三方 View 想参与换肤：`implements ViewsMatch` + `skinnableView()` 内读 `SkinManager` 单例。

### 4.6 皮肤包构建

`skinpackage` 是一个独立 `com.android.application` 模块，产出皮肤 APK：

```bash
./gradlew :skinpackage:assembleRelease
# 产物：skinpackage/build/outputs/apk/release/skinpackage-release-unsigned.apk
# 复制改名为 skinlibrary/src/main/assets/skin/skindemo.skin
```

宿主启动时 `AssetsUtils.doCopy` 把 `assets/skin/` 拷到 `getExternalFilesDir("skindemo")`，再 `loadSkin` 加载。已验证 `skindemo.skin` 与 `skinpackage-release-unsigned.apk` 逐字节一致（仅后缀不同）。

### 4.7 排查

```bash
adb logcat | grep "\[Skin\]"                          # 全部主题日志
adb logcat | grep -E "\[Skin\](SkinManager|SkinActivity)"  # 切肤主链路
adb logcat | grep "皮肤包缺少同名资源"                    # 「某资源没变」的 #1 根因
```

日志级别（`SkinLog` 门面，TAG 前缀统一 `[Skin]`）：
- `e`/`w`：错误 / 降级回退（永远打印）
- `i`：关键状态切换（初始化 / 切肤 / 加载皮肤包）
- `d`：过程细节，受 `SkinLog.debugEnabled` 门控（默认 `BuildConfig.DEBUG`，可运行时强制开启）

### 4.8 常见坑速查

| 现象 | 根因 | 处理 |
|---|---|---|
| 切肤后某资源不变 | 皮肤包缺同名资源 | 打 `[Skin]` warn 日志，补同名资源 |
| 弹框不跟随换肤 | 独立 Window 未注册 | `SkinManager.registerWindow(root)` |
| 代码设色不生效 | 用了字面量色值（非资源 ID） | 改用 `setTextColor(context.getColor(R.color.x))` |
| 跟随系统时页面卡在旧深浅色 | 跟随系统路径强制了 `YES/NO` | `forceNightMode=false` 时重置 `FOLLOW_SYSTEM` |
| 多库 Factory 冲突 | 各自 `setFactory2` | 改走 `LayoutFactoryRegistry.register` |
