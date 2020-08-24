package com.kylin.skinlibrary.utils

import android.R
import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi

object StatusBarUtils {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun forStatusBar(activity: Activity, skinColor: Int) {
        activity.window.statusBarColor = skinColor
    }
}