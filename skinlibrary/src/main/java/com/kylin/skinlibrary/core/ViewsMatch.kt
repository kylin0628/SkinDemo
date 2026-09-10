package com.netease.skin.library.core

/**
 * 可换肤控件契约。
 *
 * 实现本接口的控件会被切肤遍历（[com.netease.skin.library.base.SkinActivity.applyViews] /
 * [com.kylin.skinlibrary.SkinManager.applySkin]）识别，并调用 [skinnableView] 按当前皮肤刷新。
 * 每个实现须在自己的构造阶段用 [com.kylin.skinlibrary.model.AttrsBean] 记录要换肤属性的资源 ID，
 * 在 [skinnableView] 里读回并交给 [com.kylin.skinlibrary.SkinManager] 按名映射后重新赋值。
 */
interface ViewsMatch {
    /** 按当前皮肤刷新本控件（颜色 / 图片 / 字符串 / 尺寸等按名映射到皮肤包同名资源）。 */
    fun skinnableView()
}