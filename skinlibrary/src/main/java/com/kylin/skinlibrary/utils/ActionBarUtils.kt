package com.kylin.skinlibrary.utils

import android.R
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.withStyledAttributes

object ActionBarUtils {
    fun forActionBar(activity: AppCompatActivity) {
        var color = 0
        activity.withStyledAttributes(attrs = intArrayOf(R.attr.colorPrimary)) { color = getColor(0, 0) }
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
    }

    fun forActionBar(activity: AppCompatActivity, skinColor: Int) {
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(skinColor))
    }
}
