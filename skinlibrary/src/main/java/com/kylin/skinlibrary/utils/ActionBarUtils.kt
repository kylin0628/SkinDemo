package com.kylin.skinlibrary.utils

import android.R
import android.annotation.TargetApi
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

object ActionBarUtils {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun forActionBar(activity: AppCompatActivity) {
        val a = activity.theme.obtainStyledAttributes(
            0, intArrayOf(
                R.attr.colorPrimary
            )
        )
        val color = a.getColor(0, 0)
        a.recycle()
        val actionBar = activity.supportActionBar
        actionBar?.setBackgroundDrawable(ColorDrawable(color))
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun forActionBar(activity: AppCompatActivity, skinColor: Int) {
        val actionBar = activity.supportActionBar
        actionBar?.setBackgroundDrawable(ColorDrawable(skinColor))
    }
}