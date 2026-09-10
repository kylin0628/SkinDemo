package com.netease.skin.library.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.LayoutInflaterCompat
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.SkinnableInflaterFactory

/**
 * 无继承的换肤 inflate 入口。
 *
 * 不依赖继承 [SkinDialog] / [SkinDialogFragment] / [SkinPopupWindow] / [SkinActivity]，
 * 对任意 [Context]（原生 Dialog / PopupWindow / Fragment / 自定义容器）一行调用即可：
 *   1. 克隆独立 inflater 并挂 Factory2，把 XML 标签名替换成 Skinnable* 控件；
 *   2. 立即按当前皮肤刷一遍；
 *   3. 可选调用 [SkinManager.registerWindow] 注册独立窗口根视图，切肤时自动跟随。
 *
 * 与继承基类等价，供「已有基类 / 想解耦继承」的场景选用，二者可并存混用。
 */
object SkinnableViewInflater {

    /**
     * 换肤 inflate 一个布局。
     *
     * @param registerWindow 是否注册为独立窗口（PopupWindow/Dialog 的根视图应传 true，
     *                       切肤时由 SkinManager 自动遍历换肤）。
     */
    fun inflate(
        context: Context,
        layoutRes: Int,
        root: ViewGroup? = null,
        attachToRoot: Boolean = false,
        registerWindow: Boolean = true
    ): View {
        val inflater = LayoutInflater.from(context).cloneInContext(context)
        // 每次 new 工厂：SkinnableInflaterFactory 首帧绑定 context，跨窗口/不同主题复用会串 context。
        LayoutInflaterCompat.setFactory2(inflater, SkinnableInflaterFactory())
        val view = inflater.inflate(layoutRes, root, attachToRoot)
        SkinManager.instance?.applySkin(view)
        if (registerWindow) {
            SkinManager.instance?.registerWindow(view)
        }
        return view
    }
}
