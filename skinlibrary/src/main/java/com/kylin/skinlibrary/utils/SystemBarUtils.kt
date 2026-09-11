package com.kylin.skinlibrary.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 系统栏（状态栏 + 导航栏/dock）沉浸式与避让的统一适配工具。
 *
 * 与 [StatusBarUtils] / [NavigationUtils]（单栏着色）分工：
 * - 前者在每次换肤（skinDynamic）时刷新「背景色 + 图标深浅色」；
 * - 本类提供「一次性」的窗口模式设置——沉浸式 edge-to-edge、内容避让系统栏、手势导航探测，
 *   以及两者共用的「颜色深浅判定」。
 *
 * 设计取向：edge-to-edge 会改变内容布局（内容延伸到系统栏后面），属于宿主页面级的视觉决策，
 * 故作为**显式可选项**由宿主按需调用（[enableEdgeToEdge] + [applyInsetPadding]），
 * 而非在换肤流程里强制开启，避免破坏未按沉浸式设计的页面。
 */
object SystemBarUtils {

    /**
     * 判断颜色是否偏浅（WCAG 相对亮度 > 0.5）。
     *
     * 用于自动决定系统栏图标用深色还是浅色：背景偏浅 → 图标应换深色（否则白图标看不清）；
     * 背景偏深 → 图标保持浅色。供 [StatusBarUtils] / [NavigationUtils] 共用，避免两处重复阈值。
     */
    fun isLightColor(color: Int): Boolean = ColorUtils.calculateLuminance(color) > 0.5

    /**
     * 沉浸式 edge-to-edge：内容延伸到状态栏 / 导航栏后面绘制。
     *
     * 内部即 `WindowCompat.setDecorFitsSystemWindows(window, false)`——关闭系统默认的
     * 「内容避让系统栏」padding，让背景/内容铺满到屏幕边缘。
     *
     * 注意：只关闭默认避让、不施加避让，需配合 [applyInsetPadding] 让实际内容避开系统栏，
     * 否则滚动内容会被状态栏/导航栏遮挡。
     */
    fun enableEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    /**
     * 内容避让系统栏：把系统栏 insets 作为 padding 应用到目标视图，避免内容被遮挡。
     *
     * 典型用法：`enableEdgeToEdge(activity)` 后，对内容根视图（如
     * `findViewById(android.R.id.content)`）调用本方法，让内容整体避开状态栏/导航栏；
     * 背景仍延伸到系统栏后面，形成沉浸式效果。
     *
     * @param view 需要避让的目标视图（通常为内容根容器）
     * @param bars 需要避让的系统栏类型，默认 [WindowInsetsCompat.Type.systemBars]（状态栏+导航栏）。
     *             若页面顶部已由 ActionBar/Toolbar 自行处理状态栏 insets，可只传
     *             [WindowInsetsCompat.Type.navigationBars] 仅避让底部导航栏。
     */
    fun applyInsetPadding(view: View, bars: Int = WindowInsetsCompat.Type.systemBars()) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val padding = insets.getInsets(bars)
            v.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            insets
        }
    }

    /**
     * 是否处于手势导航模式（系统导航栏交互模式 == 2，即全手势）。
     *
     * 通过框架隐藏资源 `android:integer/config_navBarInteractionMode` 探测，值为 2 表示手势导航，
     * 0/1 分别对应三键 / 两键导航。这是系统未暴露公开 API 时的通用启发式（无手势导航的 API < 29
     * 直接返回 false）。仅作展示态判断，失败/缺失时安全回落为「非手势导航」。
     */
    fun isGestureNavigation(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resId = context.resources.getIdentifier(
            "config_navBarInteractionMode", "integer", "android"
        )
        return resId != 0 && context.resources.getInteger(resId) == 2
    }
}
