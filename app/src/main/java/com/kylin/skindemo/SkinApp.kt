package com.kylin.skindemo

import android.app.Application
import android.util.Log
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.SkinUiHost
import com.kylin.skinlibrary.utils.AssetsUtils
import com.kylin.skinlibrary.utils.PreferencesUtils
import java.io.File
import java.io.IOException

/**
 *@Description:
 *@Auther: wangqi
 * CreateTime: 2020/8/6.
 */
class SkinApp : Application() {

    companion object {
        private const val TAG = "[Skin] SkinApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "========================================")
        Log.d(TAG, "onCreate() → Application 启动")
        Log.d(TAG, "========================================")

        // 1. 初始化 SkinManager（最早时机）
        Log.d(TAG, "步骤1: 初始化 SkinManager")
        SkinManager.init(this)
        Log.d(TAG, "步骤1: SkinManager 初始化完成, instance=${SkinManager.instance}")

        // 1.5 注册宿主切肤入口钩子，供第三方模块（比亚迪演示页）挂载切肤悬浮按钮
        SkinUiHost.installThemeSwitcher = { activity ->
            ThemeSwitcher.installFab(activity)
        }

        // 2. 从持久化存储恢复上次的皮肤状态，注入 SkinManager
        restoreSkinState()

        // 3. 拷贝皮肤包资源
        try {
            Log.d(TAG, "步骤3: 拷贝皮肤包资源到外部存储")
            AssetsUtils.doCopy(
                this,
                "skin",
                "${applicationContext.getExternalFilesDir("skindemo")!!.absolutePath}"
            )
            Log.d(TAG, "步骤3: 皮肤包资源拷贝完成")
        } catch (e: IOException) {
            Log.e(TAG, "步骤3: 拷贝皮肤包资源失败!", e)
            e.printStackTrace()
        }

        Log.d(TAG, "onCreate() → 完成")
    }

    /**
     * 从 SharedPreferences 恢复上次保存的皮肤状态
     * 在 SkinActivity.onPostCreate 触发前完成注入，
     * 确保每个 Activity 创建时能自动应用正确的皮肤
     */
    private fun restoreSkinState() {
        Log.d(TAG, "步骤2: 恢复皮肤状态")
        val currentSkin = PreferencesUtils.getString(this, "currentSkin")
        Log.d(TAG, "步骤2: SharedPreferences[currentSkin]=$currentSkin")

        if ("skindemo" == currentSkin) {
            val skinPath =
                "${getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"
            Log.d(TAG, "步骤2: 上次为动态皮肤 → skinPath=$skinPath")
            SkinManager.instance?.loadSkin(skinPath, R.color.skin_item_color)
        } else {
            Log.d(TAG, "步骤2: 上次为默认皮肤（或首次启动）")
            SkinManager.instance?.loadSkin(null, R.color.colorPrimary)
        }
        Log.d(TAG, "步骤2: 皮肤状态恢复完成 → currentSkinPath=${SkinManager.instance?.currentSkinPath}")
    }
}