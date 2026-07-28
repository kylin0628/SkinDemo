package com.kylin.skindemo

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
 * 用于验证 Dialog 场景下的皮肤切换是否正常。
 * Dialog 拥有独立的 Window，LayoutInflater 不经过 SkinActivity.Factory2，
 * 因此需要自行设置 Factory2 以确保 Skinnable* 控件被正确创建。
 */
class SkinTestDialogFragment : DialogFragment(), LayoutInflater.Factory2 {

    companion object {
        private const val TAG = "[Skin] SkinTestDialog"

        fun newInstance(): SkinTestDialogFragment {
            return SkinTestDialogFragment()
        }
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

        // 关键步骤：为 Dialog 的 LayoutInflater 设置 Factory2，
        // 使得 XML 中的标准控件被替换为 Skinnable* 控件
        val dialogInflater = inflater.cloneInContext(requireContext())
        LayoutInflaterCompat.setFactory2(dialogInflater, this)

        return dialogInflater.inflate(R.layout.dialog_test_skin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() — 应用当前皮肤到 Dialog 视图树")

        // 通过宿主 SkinActivity 调用 applyViews 更新 Dialog 内控件
        val activity = requireActivity()
        if (activity is SkinActivity) {
            activity.applyViews(view)
        }

        // 更新皮肤状态显示
        updateSkinStatusLabel(view)

        // 绑定按钮事件
        setupButtons(view)
    }

    /**
     * 更新「当前皮肤」标签
     */
    private fun updateSkinStatusLabel(root: View) {
        val statusView = root.findViewById<android.widget.TextView>(R.id.tv_skin_status)
        val isDefault = SkinManager.instance?.currentSkinPath == null
        statusView?.text = if (isDefault) "当前：默认皮肤" else "当前：动态皮肤 (skindemo.skin)"
    }

    /**
     * 绑定 Dialog 内的操作按钮
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setupButtons(root: View) {
        val btnClose = root.findViewById<View>(R.id.btn_close)
        val btnDefault = root.findViewById<View>(R.id.btn_default)
        val btnDynamic = root.findViewById<View>(R.id.btn_dynamic)

        val skinPath =
            "${requireContext().getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"

        // 关闭按钮
        btnClose?.setOnClickListener {
            Log.d(TAG, "点击【关闭】按钮")
            dismiss()
        }

        // 默认主题按钮
        btnDefault?.setOnClickListener {
            Log.d(TAG, "点击 Dialog 内【默认主题】按钮")
            val activity = requireActivity()
            if (activity is SkinActivity) {
                activity.defaultSkin(R.color.colorPrimary)
                PreferencesUtils.putString(requireContext(), "currentSkin", "default")
                // Dialog 内视图也需要刷新
                activity.applyViews(root)
                updateSkinStatusLabel(root)
            }
        }

        // 动态主题按钮
        btnDynamic?.setOnClickListener {
            Log.d(TAG, "点击 Dialog 内【切换动态主题】按钮")
            val activity = requireActivity()
            if (activity is SkinActivity) {
                activity.skinDynamic(skinPath, R.color.skin_item_color)
                PreferencesUtils.putString(requireContext(), "currentSkin", "skindemo")
                // Dialog 内视图也需要刷新
                activity.applyViews(root)
                updateSkinStatusLabel(root)
            }
        }
    }

    // =================== Factory2 实现 ===================
    // 从 SkinActivity 复制而来，确保 Dialog 也能创建 Skinnable* 控件

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (!ignoreView(name)) {
            if (viewInflater == null) {
                viewInflater = CustomAppCompatViewInflater(context)
            }
            viewInflater!!.setName(name)
            viewInflater!!.setAttrs(attrs)
            val view = viewInflater!!.autoMatch()
            Log.d(TAG, "onCreateView(Factory) → $name → ${view?.javaClass?.simpleName ?: "null"}")
            return view
        }
        return null // 返回 null 让系统按默认方式创建
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return onCreateView(null, name, context, attrs)
    }

    private fun ignoreView(name: String): Boolean {
        when (name) {
            SystemViewName.FRAGMENT_CONTAINER_VIEW, SystemViewName.FRAGMENT -> return true
        }
        return false
    }
}
