package com.kylin.skindemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import com.kylin.skinlibrary.utils.PreferencesUtils
import com.netease.skin.library.base.SkinActivity
import java.io.File

class MainActivity : SkinActivity() {
    var skinPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        skinPath =
            "${applicationContext.getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"
        // 运行时权限申请（6.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (checkSelfPermission(perms[0]) === PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 200)
            }
        }

        if ("skindemo" == PreferencesUtils.getString(this, "currentSkin")) {
            skinDynamic(skinPath, R.color.skin_item_color)
        } else {
            defaultSkin(R.color.colorPrimary)
        }
    }

    // 换肤按钮（api限制：5.0版本）
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun skinDynamic(view: View) {
        // 真实项目中：需要先判断当前皮肤，避免重复操作！
        if ("skindemo" != PreferencesUtils.getString(this, "currentSkin")) {
            skinDynamic(skinPath, R.color.skin_item_color)
            PreferencesUtils.putString(this, "currentSkin", "skindemo")
        }
    }

    // 默认按钮（api限制：5.0版本）
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun skinDefault(view: View) {
        if ("default" != PreferencesUtils.getString(this, "currentSkin")) {
            defaultSkin(R.color.colorPrimary)
            PreferencesUtils.putString(this, "currentSkin", "default")
        }
    }


}