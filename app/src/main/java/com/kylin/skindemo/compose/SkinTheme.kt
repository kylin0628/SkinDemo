package com.kylin.skindemo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylin.skinlibrary.SkinManager

/**
 * Compose 换肤入口。
 *
 * 资源映射由 [SkinActivity.getResources] 覆盖（返回皮肤感知的 `SkinnableResources`）完成，
 * 因此 `colorResource` / `painterResource` / `stringResource` / `dimensionResource` 已自动按当前皮肤取值。
 *
 * 本 Composable 只负责一件事：订阅 [SkinManager.skinVersion]，在皮肤切换时用 `key(skinVersion)`
 * 强制重建整个子树——因为 Compose 的资源读取用 `remember(id, resources)` 缓存，而「动态皮肤 A → 动态皮肤 B」
 * 时 resources 对象被 SkinActivity 缓存复用、引用不变，缓存不会自行失效，需靠这里整体重建兜底。
 *
 * 用法：把每个 Compose 页面包进 [SkinTheme]。
 * ```kotlin
 * setContent { SkinTheme { MaterialTheme { /* ... */ } } }
 * ```
 */
@Composable
fun SkinTheme(content: @Composable () -> Unit) {
    var skinVersion by remember { mutableStateOf(SkinManager.instance?.skinVersion ?: 0) }

    DisposableEffect(Unit) {
        val listener = { skinVersion = SkinManager.instance?.skinVersion ?: 0 }
        SkinManager.instance?.addSkinChangeListener(listener)
        onDispose { SkinManager.instance?.removeSkinChangeListener(listener) }
    }

    // 皮肤切换 → skinVersion 变化 → 子树整体重建，所有 remember 缓存失效并重新取色
    key(skinVersion) {
        content()
    }
}
