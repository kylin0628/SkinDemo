package com.kylin.skinlibrary.utils

import android.R
import android.app.Activity

object NavigationUtils {
    fun forNavigation(activity: Activity) {
        val a = activity.theme.obtainStyledAttributes(0, intArrayOf(R.attr.statusBarColor))
        val color = a.getColor(0, 0)
        activity.window.navigationBarColor = color
        a.recycle()
    }

    fun forNavigation(activity: Activity, skinColor: Int) {
        activity.window.navigationBarColor = skinColor
    }
}
