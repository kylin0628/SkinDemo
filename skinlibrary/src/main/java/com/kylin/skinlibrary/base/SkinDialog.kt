package com.netease.skin.library.base

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.core.view.LayoutInflaterCompat
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.SkinnableInflaterFactory

/**
 * 换肤原生 Dialog 基类（对应 DialogFragment 场景的 [SkinDialogFragment]）。
 *
 * 与 DialogFragment 同理，原生 [Dialog] 拥有独立 Window，其 inflater 不经过
 * [SkinActivity] 的 Factory2。本基类把换肤样板下沉到主题库：
 *  1. 无标题 Window；
 *  2. [showWithSkin] = show + 设置 Factory2 + 换肤 inflate + setContentView；
 *  3. 首次按当前皮肤刷一遍 + [SkinManager.registerWindow] 注册独立窗口，切肤时自动跟随。
 *
 * 用法：子类继承本类，实现 [getLayoutResId]，业务写在 [onContentViewCreated]（记得先
 * `super`），调用 [showWithSkin] 展示，无需手写 Factory2 / applySkin / registerWindow 样板。
 */
abstract class SkinDialog(context: Context) : Dialog(context), LayoutInflater.Factory2 {

    private val inflaterFactory = SkinnableInflaterFactory()

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    /** 弹框布局资源 ID，子类实现。 */
    protected abstract fun getLayoutResId(): Int

    /** 展示并构建换肤内容视图：等价于 show() + 换肤 inflate + setContentView。 */
    fun showWithSkin() {
        show()
        buildContentView()
    }

    private fun buildContentView() {
        val inflater = LayoutInflater.from(context)
        val dialogInflater = inflater.cloneInContext(context)
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        val root = dialogInflater.inflate(getLayoutResId(), null)
        setContentView(root)
        // 立即按当前皮肤刷一遍 + 注册独立窗口，切肤时由 SkinManager 自动跟随换肤。
        SkinManager.instance?.applySkin(root)
        SkinManager.instance?.registerWindow(root)
        onContentViewCreated(root)
    }

    /** 内容视图构建完成后回调，子类在此绑定按钮 / 注入悬浮入口等业务。 */
    protected open fun onContentViewCreated(root: View) {}

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        inflaterFactory.createView(parent, name, context, attrs)

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        inflaterFactory.createView(null, name, context, attrs)
}
