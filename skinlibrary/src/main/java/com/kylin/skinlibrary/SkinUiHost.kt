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
}
