package com.kylin.skinlibrary.utils

import android.R
import android.app.Activity

object StatusBarUtils {
    fun forStatusBar(activity: Activity) {
        val a = activity.theme.obtainStyledAttributes(
            0, intArrayOf(
                R.attr.statusBarColor
            )
        )
        val color = a.getColor(0, 0)
        activity.window.statusBarColor = color
        a.recycle()
    }

    fun forStatusBar(activity: Activity, skinColor: Int) {
        activity.window.statusBarColor = skinColor
    }
}