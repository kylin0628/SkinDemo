package com.kylin.skindemo

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.base.SkinActivity
import com.netease.skin.library.base.SkinDialog

/**
 * 原生 Dialog 换肤案例。
 *
 * 换肤样板（Factory2 拦截 + 首次刷肤 + registerWindow 注册）已下沉到 [SkinDialog] 基类，
 * 此处只保留业务：注入悬浮切肤入口 + 绑定切肤/关闭按钮。
 */
class SkinTestDialog(context: Context) : SkinDialog(context) {

    override fun getLayoutResId(): Int = R.layout.dialog_skin_demo

    override fun onContentViewCreated(root: View) {
        // 弹框内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        (window?.decorView as? FrameLayout)?.let { ThemeSwitcher.installFabInto(it, context) }

        root.findViewById<View>(R.id.btn_dialog_close)?.setOnClickListener { dismiss() }
        root.findViewById<View>(R.id.btn_dialog_dynamic)?.setOnClickListener {
            val activity = ThemeSwitcher.findSkinActivity(context) ?: return@setOnClickListener
            val skinPath = "${activity.getExternalFilesDir("skindemo")!!.absolutePath}/skindemo.skin"
            activity.skinDynamic(skinPath)
            SkinApp.persistCurrentSkin(activity, "skindemo")
            activity.applyViews(root)
        }
        root.findViewById<View>(R.id.btn_dialog_default)?.setOnClickListener {
            val activity = ThemeSwitcher.findSkinActivity(context) ?: return@setOnClickListener
            activity.defaultSkin()
            SkinApp.persistCurrentSkin(activity, "default")
            activity.applyViews(root)
        }
    }
}
