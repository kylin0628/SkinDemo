package com.kylin.skindemo

import android.app.Application
import com.kylin.skinlibrary.utils.AssetsUtils
import com.kylin.skinlibrary.SkinManager
import java.io.IOException

/**
 *@Description:
 *@Auther: wangqi
 * CreateTime: 2020/8/6.
 */
class SkinApp : Application() {

    override fun onCreate() {
        super.onCreate()
        SkinManager.init(this)
        try {
            AssetsUtils.doCopy(
                this,
                "skin",
                "${applicationContext.getExternalFilesDir("skindemo")!!.absolutePath}"
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}