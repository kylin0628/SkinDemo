package com.kylin.skinlibrary.utils

import android.R
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity

object ActionBarUtils {
    fun forActionBar(activity: AppCompatActivity) {
        val a = activity.theme.obtainStyledAttributes(0, intArrayOf(R.attr.colorPrimary))
        val color = a.getColor(0, 0)
        a.recycle()
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
    }

    fun forActionBar(activity: AppCompatActivity, skinColor: Int) {
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(skinColor))
    }
}
