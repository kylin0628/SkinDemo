package com.kylin.skinlibrary.utils

import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity

object ActionBarUtils {
    fun forActionBar(activity: AppCompatActivity, skinColor: Int) {
        activity.supportActionBar?.setBackgroundDrawable(ColorDrawable(skinColor))
    }
}
