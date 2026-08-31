package com.kylin.skindemo

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.kylin.skinlibrary.SkinManager

/**
 * PopupWindow 换肤案例。
 *
 * PopupWindow 拥有独立 Window，不在 SkinActivity.applyViews(decorView) 覆盖范围内，
 * 故显示后调用 [SkinManager.registerWindow] 把 contentView 注册到主题库，
 * 此后每次 loadSkin 都会自动遍历该根视图换肤，业务侧无需自行管理监听器。
 */
object PopupWindowDemo {

    fun show(context: Context, anchor: View) {
        val contentView = LayoutInflater.from(context).inflate(R.layout.popup_skin_demo, null)

        val popup = PopupWindow(
            contentView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        // 外部点击可关闭
        popup.isOutsideTouchable = true
        popup.setBackgroundDrawable(ColorDrawable(0x00000000))

        // 关闭按钮
        contentView.findViewById<View>(R.id.btn_popup_close)?.setOnClickListener {
            popup.dismiss()
        }

        // 关键：注册独立窗口根视图，切肤时自动跟随换肤
        SkinManager.instance?.registerWindow(contentView)
        // 弹窗内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        ThemeSwitcher.installFabIntoPopup(popup, context)

        // 显示在锚点下方
        popup.showAsDropDown(anchor, 0, 16)
    }
}
