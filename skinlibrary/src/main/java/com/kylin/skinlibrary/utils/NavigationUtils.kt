package com.kylin.skinlibrary.utils

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat

/**
 * 导航栏（dock）换肤适配：背景色 + 图标深浅色自动适配 + 手势导航适配。
 *
 * 换肤流程（[com.netease.skin.library.base.SkinActivity.skinDynamic]）每次切肤调用
 * [forNavigation]：按主题色设置导航栏背景，按背景深浅自动切换导航栏图标深浅色；
 * 手势导航下则关闭「对比度强制」（`navigationBarContrastEnforced`），避免系统把手势条
 * 硬涂成不透明白/黑、与沉浸式背景突兀。
 */
object NavigationUtils {

    /**
     * 应用导航栏皮肤色并自动适配图标深浅色 + 手势导航。
     *
     * @param activity 目标 Activity
     * @param skinColor 导航栏背景色（由调用方按主题解析，皮肤感知）
     * @param autoLight 是否自动按背景深浅切换图标深浅色，默认开启
     */
    fun forNavigation(activity: Activity, skinColor: Int, autoLight: Boolean = true) {
        val window = activity.window
        window.navigationBarColor = skinColor
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (autoLight) {
            controller.isAppearanceLightNavigationBars = SystemBarUtils.isLightColor(skinColor)
        }
        // 手势导航适配：关闭系统「对比度强制」（Window API 29+），让手势条颜色跟随导航栏
        // 背景而非被系统强制涂成不透明白/黑，避免与沉浸式背景突兀。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    /** 仅设置导航栏背景色（不改变图标深浅 / 手势适配，兼容旧调用方语义）。 */
    @Suppress("DEPRECATION")
    @Deprecated("使用 forNavigation(activity, skinColor)，含图标深浅 + 手势导航适配")
    fun setNavigationBarColor(activity: Activity, skinColor: Int) {
        activity.window.navigationBarColor = skinColor
    }
}
