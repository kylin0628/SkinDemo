package com.kylin.skinlibrary.core

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.kylin.skinlibrary.utils.SystemViewName

/**
 * LayoutInflater.Factory2 的换肤拦截复用体。
 *
 * Dialog / DialogFragment / PopupWindow 等独立窗口拥有自己的 Window，其 `LayoutInflater`
 * 不经过 [com.netease.skin.library.base.SkinActivity] 的 Factory2，需各自设置 Factory2
 * 把 XML 标签名替换成 Skinnable* 控件。本类把这段「标签名 → 换肤控件」的拦截逻辑下沉到
 * 主题库，各窗口基类持有一个实例并把 `onCreateView` 委托给 [createView]，避免重复实现。
 *
 * 用法：窗口基类实现 `LayoutInflater.Factory2`，两个 `onCreateView` 重载直接委托 [createView]。
 */
class SkinnableInflaterFactory {
    private var viewInflater: CustomAppCompatViewInflater? = null

    /**
     * 拦截控件创建：命中换肤映射返回 Skinnable* 控件；不关心的控件返回 null 放行
     * （交后续 Factory / AppCompat 兜底处理，责任链约定与 SkinActivity 一致）。
     */
    fun createView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (ignoreView(name)) return null
        if (viewInflater == null) viewInflater = CustomAppCompatViewInflater(context)
        viewInflater?.setName(name)
        viewInflater?.setAttrs(attrs)
        return viewInflater?.autoMatch()
    }

    /** 不拦截的控件（Fragment 容器等），交系统 inflater 默认创建。 */
    private fun ignoreView(name: String): Boolean =
        name == SystemViewName.FRAGMENT_CONTAINER_VIEW || name == SystemViewName.FRAGMENT
}
