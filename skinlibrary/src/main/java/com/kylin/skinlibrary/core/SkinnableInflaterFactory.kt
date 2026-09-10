package com.kylin.skinlibrary.core

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.kylin.skinlibrary.utils.SystemViewName

/**
 * LayoutInflater.Factory2 的换肤拦截复用体。
 *
 * Dialog / DialogFragment / PopupWindow 等独立窗口拥有自己的 Window，其 `LayoutInflater`
 * 不经过 [com.netease.skin.library.base.SkinActivity] 的 Factory2，需各自设置 Factory2
 * 把 XML 标签名替换成 Skinnable* 控件。本类把这段「标签名 → 换肤控件」的拦截逻辑下沉到
 * 主题库，并自身实现 [LayoutInflater.Factory2]，可直接 `LayoutInflaterCompat.setFactory2`。
 *
 * 组合（非继承）用法：持有本类实例，把两个 `onCreateView` 重载委托给 [createView]；
 * 或直接作为 Factory2 传给 inflater（见 [com.netease.skin.library.base.SkinnableViewInflater]）。
 *
 * 注意：一个实例绑定一个 context（首帧 `CustomAppCompatViewInflater(context)` 的 context），
 * 跨多个 Window / 不同 context 复用时请各自 new 实例。
 */
class SkinnableInflaterFactory : LayoutInflater.Factory2 {
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

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        createView(parent, name, context, attrs)

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        createView(null, name, context, attrs)

    /** 不拦截的控件（Fragment 容器等），交系统 inflater 默认创建。 */
    private fun ignoreView(name: String): Boolean =
        name == SystemViewName.FRAGMENT_CONTAINER_VIEW || name == SystemViewName.FRAGMENT
}
