package com.kylin.skindemo

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import com.netease.skin.library.base.SkinPopupWindow

/**
 * PopupWindow 换肤案例。
 *
 * 换肤样板（Factory2 拦截 + 首次刷肤 + registerWindow 注册）已下沉到 [SkinPopupWindow] 基类，
 * 此处只保留业务：配置窗口参数 + 绑定关闭按钮 + 注入悬浮切肤入口。
 */
object PopupWindowDemo {

    fun show(context: Context, anchor: View) {
        val popup = SkinPopupWindow(context).apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            isFocusable = true
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(0x00000000))
        }
        val contentView = popup.setSkinnableContentView(R.layout.popup_skin_demo)

        // 关闭按钮
        contentView.findViewById<View>(R.id.btn_popup_close)?.setOnClickListener {
            popup.dismiss()
        }

        // 弹窗内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        ThemeSwitcher.installFabIntoPopup(popup, context)

        // 显示在锚点下方
        popup.showAsDropDown(anchor, 0, 16)
    }
}
