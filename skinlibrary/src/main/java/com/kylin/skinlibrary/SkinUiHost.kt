package com.kylin.skinlibrary

import android.app.Activity

/**
 * 主题库对宿主 UI 的可选钩子。
 *
 * 切肤入口 UI（悬浮按钮 / 切换弹框）由宿主 app 实现（依赖 app 的资源与业务色值），
 * 主题库（skinlibrary）不持有。第三方模块（如 bydwidget）想在自己的页面内挂载
 * 切肤入口时，通过本钩子回调宿主实现，避免反向依赖 app 模块。
 *
 * 用法：宿主在 Application 启动时注册一次；第三方页面调 [installThemeSwitcher]。
 */
object SkinUiHost {
    /** 宿主提供的「给 Activity 安装切肤入口」实现；未注册则跳过 */
    @Volatile
    var installThemeSwitcher: ((Activity) -> Unit)? = null

    /**
     * 宿主提供的「应用一套主题」统一策略。
     *
     * @param host          当前换肤宿主（继承版 [com.netease.skin.library.base.SkinActivity]
     *                      或组合版 [com.netease.skin.library.base.SkinActivityDelegate]）。
     * @param isDark        true=深色，false=浅色
     * @param forceNightMode true=用户主动切换（强制夜间模式 YES/NO，BYD 弹框/控件随 uiMode 跟随）；
     *                       false=跟随系统变化（重置夜间模式为 FOLLOW_SYSTEM，仅按 isDark 换肤）。
     *
     * 由两处复用同一实现，保证「app 内切换」与「跟随系统变化」行为一致：
     *  1. [com.netease.skin.library.base.SkinActivity.onDarkModeChanged] 转发系统深浅色变化（forceNightMode=false）；
     *  2. 宿主切肤入口（悬浮按钮弹框/按钮）在用户点选「默认/动态主题」时调用（forceNightMode=true）。
     *
     * 关键：跟随系统时不能强制 YES/NO，否则会污染全局默认夜间模式，导致无
     * `configChanges="uiMode"` 的页面（如比亚迪演示页）重建时读到被强制的旧值、卡在错误深浅色。
     * 策略下沉到宿主一处维护：皮肤包路径在宿主侧。未注册则深浅色切换不联动皮肤。
     */
    @Volatile
    var applyTheme: ((SkinnableThemeHost, Boolean, Boolean) -> Unit)? = null
}
