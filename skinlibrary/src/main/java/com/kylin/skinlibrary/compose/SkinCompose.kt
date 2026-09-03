package com.kylin.skinlibrary.compose

import android.util.TypedValue
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import com.kylin.skinlibrary.SkinManager

/**
 * Compose 换肤支持：皮肤版本信号 + 皮肤感知的资源读取。
 *
 * ## 为什么不用 `key(skinVersion)` 整体重建
 *
 * `key()` 换键会让 Compose 把整棵子树销毁重建，`remember { mutableStateOf(...) }` 里的页面
 * 状态（输入框文本、开关勾选、滑块值、滚动位置）全部丢失——这是「切主题后输入内容消失」的
 * 根因。
 *
 * Compose 官方 `colorResource` / `stringResource` / `dimensionResource` 都**不缓存资源值**
 * （每次重组重读 `LocalContext.current.resources`），只有 `painterResource` 因解析开销大而
 * 缓存。因此正确做法是：只提供一个「不销毁状态的局部重组触发信号」，让用到皮肤资源的组合
 * 函数局部重组，其余状态原样保留。
 *
 * ## 资源覆盖范围
 *
 * 颜色 / 图片 / 字符串 / 尺寸 属于「主题相关」，由 [com.kylin.skinlibrary.SkinnableResources]
 * 按名映射到皮肤包。整数 / 布尔 / 数组是业务数据、非主题范畴，本文件**不**为其提供换肤入口，
 * 以免误导业务方把它们当成可换肤资源。
 *
 * ## 用法
 *
 * 1. 页面根部包一层 [SkinComposeProvider]（订阅皮肤变化，把版本写入 [LocalSkinVersion]）。
 * 2. 资源读取改用 `skinned*` 组合函数：颜色/字符串/尺寸内部读 [LocalSkinVersion] 触发局部
 *    重组；图片用 [skinnedPainter] 单独按皮肤失效缓存。
 */
@Immutable
data class SkinVersion(val value: Int)

/**
 * 当前皮肤版本。皮肤每切换一次，[SkinManager.skinVersion] 自增，本值随之变化，触发读取该
 * 值的组合函数局部重组。用 staticCompositionLocalOf 是刻意的：它只在读取点触发重组，
 * 而不是让整个提供者子树因 key 变化重建。
 */
val LocalSkinVersion = staticCompositionLocalOf { SkinVersion(0) }

/**
 * 订阅皮肤变化并把版本写入 [LocalSkinVersion] 的提供者。
 *
 * 放到 Compose 页面根部（通常包在 `MaterialTheme` 外层）。它**不重建子树**，只更新
 * CompositionLocal 的值，因此页面状态（输入框文本等）在切肤时保持不变。
 */
@Composable
fun SkinComposeProvider(content: @Composable () -> Unit) {
    var version by remember { mutableStateOf(SkinManager.instance?.skinVersion ?: 0) }
    DisposableEffect(Unit) {
        val listener = { version = SkinManager.instance?.skinVersion ?: 0 }
        SkinManager.instance?.addSkinChangeListener(listener)
        onDispose { SkinManager.instance?.removeSkinChangeListener(listener) }
    }
    CompositionLocalProvider(LocalSkinVersion provides SkinVersion(version)) {
        content()
    }
}

/** 读取当前皮肤版本并建立重组订阅：皮肤切换时使调用点重组。 */
@Composable
@ReadOnlyComposable
private fun currentSkinVersion(): Int = LocalSkinVersion.current.value

/**
 * 皮肤感知的颜色。等价于 [colorResource]，但皮肤切换时局部重组，且经
 * [com.kylin.skinlibrary.SkinnableResources] 按名映射到皮肤包同名颜色。
 */
@Composable
@ReadOnlyComposable
fun skinnedColor(@ColorRes id: Int): Color {
    currentSkinVersion() // 仅建立重组订阅，不缓存颜色值
    return colorResource(id)
}

/** 皮肤感知的字符串。等价于 [stringResource]。 */
@Composable
@ReadOnlyComposable
fun skinnedString(@StringRes id: Int): String {
    currentSkinVersion()
    return stringResource(id)
}

@Composable
@ReadOnlyComposable
fun skinnedString(@StringRes id: Int, vararg formatArgs: Any): String {
    currentSkinVersion()
    return stringResource(id, *formatArgs)
}

/** 皮肤感知的尺寸（dp）。等价于 [dimensionResource]。 */
@Composable
@ReadOnlyComposable
fun skinnedDimension(@DimenRes id: Int): Dp {
    currentSkinVersion()
    return dimensionResource(id)
}

/**
 * 皮肤感知的图片。
 *
 * `painterResource` 对矢量图用全局 `ImageVectorCache`（key = theme + id，不含皮肤版本）缓存，
 * 皮肤切换后同名 drawable 内容变了也不会失效。故这里绕过该缓存：直接调用非缓存版的
 * `ImageVector.vectorResource(theme, res, id)` 重新解析，并以皮肤版本 + 资源 ID 为
 * `remember` 键，皮肤切换即重建 Painter——仅图片自身失效，不影响兄弟控件状态。
 *
 * 皮肤包需提供同名 drawable；缺名时 `res.getDrawable` 经 SkinManager 回退宿主同名图。
 */
@Composable
fun skinnedPainter(@DrawableRes id: Int): Painter {
    val version = currentSkinVersion()
    val context = LocalContext.current
    val res = context.resources // 非默认皮肤时为 SkinnableResources
    val value = remember { TypedValue() }
    res.getValue(id, value, true)
    val path = value.string
    return if (path?.endsWith(".xml") == true) {
        val imageVector = remember(version, id) {
            ImageVector.vectorResource(context.theme, res, id)
        }
        rememberVectorPainter(imageVector)
    } else {
        val imageBitmap = remember(version, id, context.theme) {
            (res.getDrawable(id, null) as android.graphics.drawable.BitmapDrawable)
                .bitmap.asImageBitmap()
        }
        BitmapPainter(imageBitmap)
    }
}
