package com.kylin.skinlibrary.utils

import android.app.Activity

object StatusBarUtils {
    fun forStatusBar(activity: Activity, skinColor: Int) {
        activity.window.statusBarColor = skinColor
    }
}