# skinlibrary 主题库

运行时皮肤包换肤框架。宿主 App 固定 `MODE_NIGHT_NO`（浅色），通过加载独立皮肤包 APK（`dark.skin`）切换主题，按资源「同名映射」替换颜色 / 图片 / 字符串 / 尺寸。

## 一、四维资源适配能力

| 维度 | 映射方式 | 取值优先级 | 状态 |
|---|---|---|---|
| **颜色** | `color` / `colorStateList` | 皮肤包同名 → 宿主 | ✅ 完整 |
| **图片** | `drawable` / `mipmap` | 皮肤包同名 → 宿主 | ✅ 完整（需皮肤包提供同名资源） |
| **字符串** | `string` / `text` | 皮肤包同名 → 宿主（locale 感知） | ✅ 完整 |
| **尺寸/整数/布尔** | `dimen` / `integer` / `bool` | 皮肤包同名 → 宿主 | ✅ 完整 |
| **语言** | 宿主 `values-*` 多语言 | 跟随系统 locale | ✅ 宿主侧（非皮肤包） |

**核心语义**：所有资源按「名称」映射。皮肤包必须提供与宿主**同名**的资源才生效；缺名则回退宿主（并打 `[Skin]` warn 日志：`皮肤包缺少同名资源 → type/name`）。

## 二、资源门面（SkinManager）

```kotlin
SkinManager.instance?.let { m ->
    m.getColor(id)                    // 颜色
    m.getColorStateList(id)           // 颜色选择器
    m.getDrawableOrMipMap(id)         // 图片（drawable/mipmap）
    m.getString(id)                   // 字符串
    m.getString(id, *args)            // 带格式化
    m.getText(id)                     // CharSequence
    m.getDimension(id)                // 尺寸 Float
    m.getDimensionPixelSize(id)       // 尺寸 Int(px)
    m.getInteger(id)                  // 整数
    m.getBoolean(id)                  // 布尔
    m.resolveSkinId(id)               // 宿主ID → 皮肤包ID（0=缺名）
    m.getSkinResourcesOrNull()        // 皮肤包 Resources（非默认皮肤）
}
```

## 三、接入方式

### 1. 原生 View（XML 换肤）

- Activity 继承 `SkinActivity`（项目内已统一由 `BaseActivity` 继承）
- XML 里的 `TextView`/`Button`/`ImageView` 等被 Factory2 自动替换为 `Skinnable*` 子类，记录 `background`/`textColor`/`src` 等属性，切肤时遍历 `ViewsMatch` 重刷
- **多库 Factory 兼用**：第三方若也要拦截控件，改调 `LayoutFactoryRegistry.register(factory)`（不自行 `setFactory2`），见 [Factory 责任链契约](#六factory-责任链契约)

### 2. Compose

把每个 Compose 入口包进 `SkinTheme`：

```kotlin
setContent { SkinTheme { MaterialTheme { /* ... */ } } }
```

`SkinTheme` 会把 `LocalContext` 换成 `SkinnableContext`，使 `colorResource` / `painterResource` / `stringResource` / `dimensionResource` **自动按当前皮肤取值，业务代码零改动**。已覆盖的入口：`PlayerActivity` / `FeedbackActivity` / `VIPInfoDialogFragment` / `LoginSuccessRewardDialogFragment` / `LoginDialogFragmentActivityRules` / `PlayerSpeedDialogFragment`。

> 新增 Compose 页面时记得包一层 `SkinTheme`。

### 3. 独立窗口（PopupWindow / Dialog）

弹框显示后调一次 `SkinManager.instance?.registerWindow(rootView)`，切肤时自动遍历换肤。

## 四、皮肤包构建

```bash
./gradlew :SkinDark:packageSkin
```

产物：`CarBase/src/main/assets/skin/dark.skin`。宿主 `preBuild` 已依赖该任务自动打包。

## 五、语言适配（跟随系统 locale）

语言走**宿主 Android 原生多语言**，非皮肤包：

```
Main/src/main/res/values/strings.xml       # 默认（中文）
Main/src/main/res/values-en/strings.xml    # 英文
Main/src/main/res/values-ja/strings.xml    # 日文（按需）
```

同名 string 会被系统按当前 locale 自动选中。**主题库无需改动语言逻辑**；`SkinManager.loaderSkinResources` 创建皮肤包 Resources 时沿用 `appResources.configuration`（携带 locale），皮肤包若含 `values-*` 同样按 locale 解析。

## 六、Factory 责任链契约

framework 的 `LayoutInflater.setFactory2` **只能设一次**（第二次抛 `IllegalStateException`），故多库并存必须走责任链：

1. 第三方库**不自行调 `LayoutInflaterCompat.setFactory2`**，改调 `LayoutFactoryRegistry.register(factory)`
2. 每个 factory 对不关心的 View **返回 `null` 放行**（否则吃掉链，后面的库收不到 View）
3. 同类控件「先注册者优先」，后来者对该类控件静默失效
4. 第三方 View 想参与换肤：`implements ViewsMatch` + `skinnableView()` 内读 `SkinManager` 单例

## 七、新增一种「可换肤资源/属性」标准步骤

1. `SkinManager` 加对应 getter（复用 `getSkinResourceIds` + `useHost` 模式）
2. 原生侧：`attrs.xml` 加 `declare-styleable` 属性 + 对应 `Skinnable*.skinnableView()` 分支
3. Compose 侧：`SkinnableResources` 加对应 override（若 Compose 资源 API 走 `LocalContext.current.resources`）
4. 皮肤包 `SkinDark` 提供同名资源
5. 编译 `:skinlibrary :Main`

## 八、排查

```bash
adb logcat | grep "\[Skin\]"                          # 全部主题日志
adb logcat | grep -E "\[Skin\](SkinManager|SkinActivity)"  # 切肤主链路
adb logcat | grep "皮肤包缺少同名资源"                    # 「某资源没变」的 #1 根因
```

日志级别：`i`=关键流程里程碑（始终打印）；`w`=异常回退；`e`=异常；`d`=细节（受 `SkinLog.debugEnabled` 门控，默认 `BuildConfig.DEBUG`）。
