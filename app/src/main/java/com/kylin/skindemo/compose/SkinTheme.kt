package com.kylin.skindemo.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.kylin.skindemo.R
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.compose.SkinComposeProvider

/**
 * Compose 换肤入口（兼容包装）。
 *
 * 资源映射由 [com.kylin.skinlibrary.SkinnableResources]（SkinActivity.getResources 覆盖）完成，
 * 因此 `colorResource` / `painterResource` / `stringResource` / `dimensionResource` 会自动按当前皮肤取值。
 *
 * 真正的重组信号下沉到主题库的 [SkinComposeProvider]：它通过 CompositionLocal 提供皮肤版本，
 * 使所有资源读取点（`colorResource`/`stringResource`/`painterResource` 等）**局部重组**，而不是
 * 用 `key(skinVersion)` 整体重建子树——从而保证切主题后输入框文本、开关勾选、滑块值等页面状态
 * 保持不变（详见 SkinCompose.kt 顶部注释）。
 *
 * 用法：把每个 Compose 页面包进 [SkinTheme]。
 * ```kotlin
 * setContent { SkinTheme { MaterialTheme(colorScheme = skinColorScheme()) { /* ... */ } } }
 * ```
 */
@Composable
fun SkinTheme(content: @Composable () -> Unit) {
    SkinComposeProvider(content)
}

/**
 * 皮肤感知的 Material3 [ColorScheme]。
 *
 * Compose 的**原生主题适配方式**：不逐个给组件传 `colors = XxxDefaults.colors(...)`，
 * 而是把皮肤色映射到 [androidx.compose.material3.MaterialTheme] 的 `colorScheme` 语义槽位，
 * 让 Button / Switch / Checkbox / RadioButton / Slider / Card / OutlinedTextField 等组件
 * 自动从主题取色。业务代码保持 `Switch(checked, onCheckedChange)` 这类原生写法不变。
 *
 * 皮肤切换时 [com.kylin.skinlibrary.compose.LocalSkinVersion] 变化 → 本函数（以及页面内每个
 * 资源读点）局部重组 → 重建 ColorScheme → 依赖它的组件自动换色，页面状态不丢。
 *
 * 语义槽位映射（宿主浅色 ↔ 皮肤暗色）：
 *  - primary/surfaceVariant 等槽位对应 skin_bridge/语义色，皮肤包提供同名色按名覆盖。
 *  - onPrimary 固定白：主题色（蓝/橙）上白字对比度最优。
 */
@Composable
fun skinColorScheme(): ColorScheme {
    // 逐个读皮肤色（系统 colorResource，经 SkinnableResources 按名映射到皮肤包）
    val primary = colorResource(R.color.skin_item_color)
    val background = colorResource(R.color.dialog_bg)
    val surface = colorResource(R.color.dialog_bg)
    val surfaceVariant = colorResource(R.color.dialog_button_bg)
    val onBackground = colorResource(R.color.text_primary)
    val onSurface = colorResource(R.color.text_primary)
    val onSurfaceVariant = colorResource(R.color.text_hint)
    val outline = colorResource(R.color.text_hint)

    // 默认皮肤 = 浅色方案；皮肤包 = 暗色方案（isLight 影响 ripple / elevation tone 等）
    val isDark = SkinManager.instance?.isDefaultSkin == false
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
        )
    }
}
