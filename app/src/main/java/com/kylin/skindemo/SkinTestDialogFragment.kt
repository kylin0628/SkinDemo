package com.kylin.skindemo

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.LayoutInflaterCompat
import androidx.fragment.app.DialogFragment
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.CustomAppCompatViewInflater
import com.kylin.skinlibrary.utils.PreferencesUtils
import com.kylin.skinlibrary.utils.SystemViewName
import com.netease.skin.library.base.SkinActivity
import java.io.File

/**
 * 换肤测试 DialogFragment
 *
 * Dialog 拥有独立的 Window，LayoutInflater 不经过 SkinActivity.Factory2，
 * 因此自行设置 Factory2 以确保 Skinnable* 控件被正确创建。
 */
class SkinTestDialogFragment : DialogFragment(), LayoutInflater.Factory2 {

    companion object {
        private const val TAG = "[Skin] SkinTestDialog"
        fun newInstance() = SkinTestDialogFragment()
    }

    private var viewInflater: CustomAppCompatViewInflater? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() — 设置 Dialog 的 Factory2")
        val dialogInflater = inflater.cloneInContext(requireContext())
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        return dialogInflater.inflate(R.layout.dialog_test_skin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() — 应用当前皮肤到 Dialog 视图树")
        val activity = requireActivity()
        if (activity is SkinActivity) {
            activity.applyViews(view)
        }
        // 弹框内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        (dialog?.window?.decorView as? FrameLayout)?.let {
            ThemeSwitcher.installFabInto(it, requireContext())
        }
        updateSkinStatusLabel(view)
        setupButtons(view)
    }

    private fun updateSkinStatusLabel(root: View) {
        val statusView = root.findViewById<android.widget.TextView>(R.id.tv_skin_status)
        val isDefault = SkinManager.instance?.currentSkinPath == null
        statusView?.text = if (isDefault) "当前：默认皮肤" else "当前：动态皮肤 (skindemo.skin)"
    }

    private fun setupButtons(root: View) {
        val btnClose = root.findViewById<View>(R.id.btn_close)
        val btnDefault = root.findViewById<View>(R.id.btn_default)
        val btnDynamic = root.findViewById<View>(R.id.btn_dynamic)
        val skinPath = "${requireContext().getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"

        btnClose?.setOnClickListener {
            Log.d(TAG, "点击【关闭】按钮")
            dismiss()
        }

        btnDefault?.setOnClickListener {
            Log.d(TAG, "点击 Dialog 内【默认主题】按钮")
            val activity = requireActivity()
            if (activity is SkinActivity) {
                activity.defaultSkin(R.color.colorPrimary)
                PreferencesUtils.putString(requireContext(), "currentSkin", "default")
                activity.applyViews(root)
                updateSkinStatusLabel(root)
            }
        }

        btnDynamic?.setOnClickListener {
            Log.d(TAG, "点击 Dialog 内【切换动态主题】按钮")
            val activity = requireActivity()
            if (activity is SkinActivity) {
                activity.skinDynamic(skinPath, R.color.skin_item_color)
                PreferencesUtils.putString(requireContext(), "currentSkin", "skindemo")
                activity.applyViews(root)
                updateSkinStatusLabel(root)
            }
        }
    }

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (!ignoreView(name)) {
            if (viewInflater == null) viewInflater = CustomAppCompatViewInflater(context)
            viewInflater?.setName(name)
            viewInflater?.setAttrs(attrs)
            val view = viewInflater?.autoMatch()
            Log.d(TAG, "onCreateView(Factory) → $name → ${view?.javaClass?.simpleName ?: "null"}")
            return view
        }
        return null
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        onCreateView(null, name, context, attrs)

    private fun ignoreView(name: String): Boolean =
        name == SystemViewName.FRAGMENT_CONTAINER_VIEW || name == SystemViewName.FRAGMENT
}
