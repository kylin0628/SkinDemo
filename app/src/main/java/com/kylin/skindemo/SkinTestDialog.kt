package com.kylin.skindemo

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.core.view.LayoutInflaterCompat
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.CustomAppCompatViewInflater
import com.netease.skin.library.base.SkinActivity

/**
 * 原生 Dialog 换肤案例。
 *
 * 与 DialogFragment 同理，Dialog 拥有独立 Window。此处自行设置 Factory2 确保 Skinnable* 控件
 * 正确创建，并调用 [SkinManager.registerWindow] 让切肤时自动跟随换肤。
 */
class SkinTestDialog(context: Context) : Dialog(context), LayoutInflater.Factory2 {

    private var viewInflater: CustomAppCompatViewInflater? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    fun showWithSkin() {
        show()
        buildContentView()
    }

    private fun buildContentView() {
        val inflater = LayoutInflater.from(context)
        val dialogInflater = inflater.cloneInContext(context)
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        val root = dialogInflater.inflate(R.layout.dialog_skin_demo, null)
        setContentView(root)

        // 立即按当前皮肤刷一遍
        ThemeSwitcher.findSkinActivity(context)?.applyViews(root)
        // 关键：注册独立窗口根视图，切肤时自动跟随换肤
        SkinManager.instance?.registerWindow(root)
        // 弹框内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        (window?.decorView as? FrameLayout)?.let { ThemeSwitcher.installFabInto(it, context) }

        root.findViewById<View>(R.id.btn_dialog_close)?.setOnClickListener { dismiss() }
        root.findViewById<View>(R.id.btn_dialog_dynamic)?.setOnClickListener {
            val activity = ThemeSwitcher.findSkinActivity(context) ?: return@setOnClickListener
            val skinPath = "${activity.getExternalFilesDir("skindemo")!!.absolutePath}/skindemo.skin"
            activity.skinDynamic(skinPath, R.color.skin_item_color)
            com.kylin.skinlibrary.utils.PreferencesUtils.putString(activity, "currentSkin", "skindemo")
            activity.applyViews(root)
        }
        root.findViewById<View>(R.id.btn_dialog_default)?.setOnClickListener {
            val activity = ThemeSwitcher.findSkinActivity(context) ?: return@setOnClickListener
            activity.defaultSkin(R.color.colorPrimary)
            com.kylin.skinlibrary.utils.PreferencesUtils.putString(activity, "currentSkin", "default")
            activity.applyViews(root)
        }
    }

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: android.util.AttributeSet): View? {
        if (name == "fragment" || name == "androidx.fragment.app.FragmentContainerView") return null
        if (viewInflater == null) viewInflater = CustomAppCompatViewInflater(context)
        viewInflater!!.setName(name)
        viewInflater!!.setAttrs(attrs)
        return viewInflater!!.autoMatch()
    }

    override fun onCreateView(name: String, context: Context, attrs: android.util.AttributeSet): View? =
        onCreateView(null, name, context, attrs)
}
