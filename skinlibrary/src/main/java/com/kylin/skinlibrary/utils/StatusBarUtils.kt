package com.kylin.skinlibrary.utils

import android.app.Activity
import androidx.core.view.WindowCompat

/**
 * 状态栏换肤适配：背景色 + 图标深浅色自动适配。
 *
 * 换肤流程（[com.netease.skin.library.base.SkinActivity.skinDynamic]）每次切肤调用
 * [forStatusBar]：先按主题色设置状态栏背景，再按背景深浅自动切换状态栏图标为深/浅色，
 * 解决「浅色主题下白图标看不清」的经典问题。
 */
object StatusBarUtils {

    /**
     * 应用状态栏皮肤色并自动适配图标深浅色。
     *
     * @param activity 目标 Activity
     * @param skinColor 状态栏背景色（由调用方按主题解析，皮肤感知）
     * @param autoLight 是否自动按背景深浅切换图标深浅色，默认开启
     */
    fun forStatusBar(activity: Activity, skinColor: Int, autoLight: Boolean = true) {
        val window = activity.window
        window.statusBarColor = skinColor
        if (autoLight) {
            val lightIcons = SystemBarUtils.isLightColor(skinColor)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = lightIcons
        }
    }

    /** 仅设置状态栏背景色（不改变图标深浅，兼容旧调用方语义）。 */
    @Suppress("DEPRECATION")
    @Deprecated("使用 forStatusBar(activity, skinColor)，含图标深浅自动适配")
    fun setStatusBarColor(activity: Activity, skinColor: Int) {
        activity.window.statusBarColor = skinColor
    }
}
