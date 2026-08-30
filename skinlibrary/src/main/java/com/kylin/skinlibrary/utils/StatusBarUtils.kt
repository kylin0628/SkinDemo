package com.kylin.skinlibrary.utils

import android.R
import android.app.Activity
import androidx.core.content.withStyledAttributes

object StatusBarUtils {
    fun forStatusBar(activity: Activity) {
        var color = 0
        activity.withStyledAttributes(attrs = intArrayOf(R.attr.statusBarColor)) { color = getColor(0, 0) }
        activity.window.statusBarColor = color
    }

    fun forStatusBar(activity: Activity, skinColor: Int) {
        activity.window.statusBarColor = skinColor
    }
}