package com.kylin.skinlibrary.utils

import android.app.Activity

object NavigationUtils {
    fun forNavigation(activity: Activity, skinColor: Int) {
        activity.window.navigationBarColor = skinColor
    }
}
