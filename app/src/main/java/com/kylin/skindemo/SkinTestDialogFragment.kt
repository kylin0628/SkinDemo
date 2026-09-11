package com.kylin.skindemo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.base.SkinActivity
import com.netease.skin.library.base.SkinDialogFragment
import java.io.File

/**
 * 换肤测试 DialogFragment
 *
 * 换肤样板（Factory2 拦截 + 首次刷肤）已下沉到 [SkinDialogFragment] 基类，
 * 此处只保留弹框业务：注入悬浮切肤入口、刷新状态文案、绑定切肤/关闭按钮。
 */
class SkinTestDialogFragment : SkinDialogFragment() {

    companion object {
        private const val TAG = "[Skin] SkinTestDialog"
        fun newInstance() = SkinTestDialogFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme)
    }

    override fun getLayoutResId(): Int = R.layout.dialog_test_skin

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() — 应用当前皮肤到 Dialog 视图树")
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
                activity.defaultSkin()
                SkinApp.persistCurrentSkin("default")
                activity.applyViews(root)
                updateSkinStatusLabel(root)
            }
        }

        btnDynamic?.setOnClickListener {
            Log.d(TAG, "点击 Dialog 内【切换动态主题】按钮")
            val activity = requireActivity()
            if (activity is SkinActivity) {
                activity.skinDynamic(skinPath)
                SkinApp.persistCurrentSkin("skindemo")
                activity.applyViews(root)
                updateSkinStatusLabel(root)
            }
        }
    }
}
