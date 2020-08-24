package com.kylin.skinlibrary.utils

import android.R
import android.annotation.TargetApi
import android.app.Activity
import android.os.Build

object NavigationUtils {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun forNavigation(activity: Activity) {
        val a = activity.theme.obtainStyledAttributes(
            0, intArrayOf(
                R.attr.statusBarColor
            )
        )
        val color = a.getColor(0, 0)
        activity.window.navigationBarColor = color
        a.recycle()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun forNavigation(activity: Activity, skinColor: Int) {
        activity.window.navigationBarColor = skinColor
    }
}