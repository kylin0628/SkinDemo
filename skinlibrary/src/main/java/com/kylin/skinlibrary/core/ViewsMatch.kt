package com.netease.skin.library.core

/**
 * 控件匹配接口，仅用于匹配后的执行方法
 */
interface ViewsMatch {
    /**
     * 控件换肤
     * isDefultSkin当前控件是否支持主题切换，切记这个控件一定要在每个控件赋值
     */
    fun skinnableView()
}