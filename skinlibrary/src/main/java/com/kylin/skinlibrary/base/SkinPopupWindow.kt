package com.netease.skin.library.base

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.LayoutInflaterCompat
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.SkinnableInflaterFactory

/**
 * 换肤 PopupWindow 基类。
 *
 * PopupWindow 拥有独立 Window，其 contentView 若用普通 inflate（不带 Factory2）则内部的
 * TextView / Button 不会被替换成 Skinnable* 控件，[SkinManager.registerWindow] 也会因找不到
 * [com.netease.skin.library.core.ViewsMatch] 而空转。本基类把换肤样板下沉到主题库：
 *  [setSkinnableContentView] 用带 Factory2 的 inflater inflate 布局、设为 contentView、
 *  并按当前皮肤刷一遍 + 注册独立窗口，切肤时自动跟随。
 *
 * 用法：构造 [SkinPopupWindow] 后配置尺寸/背景/焦点等，调用 [setSkinnableContentView] 拿到
 * 根视图绑定业务，再 `showAsDropDown` 展示。
 */
class SkinPopupWindow(context: Context) : PopupWindow(context), LayoutInflater.Factory2 {

    private val appContext: Context = context
    private val inflaterFactory = SkinnableInflaterFactory()

    /** 换肤 inflate 布局并设为 contentView，注册独立窗口，返回根视图供绑定业务。 */
    fun setSkinnableContentView(layoutResId: Int): View {
        val inflater = LayoutInflater.from(appContext)
        val dialogInflater = inflater.cloneInContext(appContext)
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        val root = dialogInflater.inflate(layoutResId, null)
        contentView = root
        // 立即按当前皮肤刷一遍 + 注册独立窗口，切肤时由 SkinManager 自动跟随换肤。
        SkinManager.instance?.applySkin(root)
        SkinManager.instance?.registerWindow(root)
        return root
    }

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        inflaterFactory.createView(parent, name, context, attrs)

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        inflaterFactory.createView(null, name, context, attrs)
}
