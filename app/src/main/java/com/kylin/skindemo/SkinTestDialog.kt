package com.kylin.skindemo

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.SkinUiHost
import com.netease.skin.library.base.SkinDialog

/**
 * 原生 Dialog 换肤案例。
 *
 * 换肤样板（Factory2 拦截 + 首次刷肤 + registerWindow 注册）已下沉到 [SkinDialog] 基类，
 * 此处只保留业务：注入悬浮切肤入口 + 绑定切肤/关闭按钮。
 *
 * 切肤走 [SkinUiHost.applyTheme] 统一策略（与 [ThemeSwitcherDialog] 一致），支持继承版
 * SkinActivity 与组合版 Activity 两种宿主；弹框自身内容由 SkinDialog 基类的 registerWindow
 * 兜底，切肤时自动跟随。
 */
class SkinTestDialog(context: Context) : SkinDialog(context) {

    override fun getLayoutResId(): Int = R.layout.dialog_skin_demo

    override fun onContentViewCreated(root: View) {
        // 弹框内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        (window?.decorView as? FrameLayout)?.let { ThemeSwitcher.installFabInto(it, context) }

        root.findViewById<View>(R.id.btn_dialog_close)?.setOnClickListener { dismiss() }
        root.findViewById<View>(R.id.btn_dialog_dynamic)?.setOnClickListener {
            applySkin(true)
        }
        root.findViewById<View>(R.id.btn_dialog_default)?.setOnClickListener {
            applySkin(false)
        }
    }

    /** 统一切肤：深色→动态皮肤 / 浅色→默认皮肤，优先走宿主 applyTheme 统一策略。 */
    private fun applySkin(isDark: Boolean) {
        val host = ThemeSwitcher.findSkinActivity(context)
        val applied = if (host != null) {
            SkinUiHost.applyTheme?.let { hook ->
                hook(host, isDark, true)
                true
            } ?: false
        } else {
            false
        }
        if (!applied && host != null) {
            // 未注册宿主统一策略时回落直接换肤。
            if (isDark) {
                val skinPath = "${context.applicationContext.getExternalFilesDir("skindemo")?.absolutePath}/skindemo.skin"
                host.skinDynamic(skinPath)
                SkinApp.persistCurrentSkin(context, "skindemo")
            } else {
                host.defaultSkin()
                SkinApp.persistCurrentSkin(context, "default")
            }
        }
    }
}
