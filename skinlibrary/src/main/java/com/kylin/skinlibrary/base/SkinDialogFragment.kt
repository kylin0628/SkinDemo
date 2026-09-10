package com.netease.skin.library.base

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.LayoutInflaterCompat
import androidx.fragment.app.DialogFragment
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.SkinnableInflaterFactory

/**
 * 换肤 DialogFragment 基类。
 *
 * Dialog 拥有独立 Window，其 `LayoutInflater` 不经过 [SkinActivity] 的 Factory2，
 * 故 Skinnable* 控件不会被自动替换。本基类把这段样板下沉到主题库：
 *  1. 设置 Factory2，把 XML 标签名替换成 Skinnable* 控件；
 *  2. `onViewCreated` 时按当前皮肤刷一遍（弹框 inflater 不经 SkinActivity.Factory2，
 *     无 inflate 即换肤，需手动刷一遍保证首次显示即正确配色）；
 *  3. 切肤时由 [SkinActivity.applyViewsToDialogs] 遍历已打开弹框自动跟随，无需自行 registerWindow。
 *
 * 用法：子类继承本类并实现 [getLayoutResId]，业务逻辑照常写在 `onViewCreated`
 * （记得先 `super.onViewCreated(...)`），无需再手写 Factory2 / applyViews 样板。
 */
abstract class SkinDialogFragment : DialogFragment(), LayoutInflater.Factory2 {

    private val inflaterFactory = SkinnableInflaterFactory()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dialogInflater = inflater.cloneInContext(requireContext())
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        return dialogInflater.inflate(getLayoutResId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 首次显示按当前皮肤刷一遍（弹框 inflater 不经 SkinActivity.Factory2，无自动 skinnableView）。
        // 直接走 SkinManager.applySkin，解耦对「宿主 Activity 必须是 SkinActivity」的依赖。
        SkinManager.instance?.applySkin(view)
    }

    /** 弹框布局资源 ID，子类实现。 */
    protected abstract fun getLayoutResId(): Int

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        inflaterFactory.createView(parent, name, context, attrs)

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        inflaterFactory.createView(null, name, context, attrs)
}
