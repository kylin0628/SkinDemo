package com.kylin.skindemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.utils.PreferencesUtils
import com.netease.skin.library.base.SkinActivity
import java.io.File

class MainActivity : SkinActivity() {

    companion object {
        private const val TAG = "[Skin] MainActivity"
    }

    var skinPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() 开始")
        setContentView(R.layout.activity_main)
        skinPath =
            "${applicationContext.getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"
        Log.d(TAG, "onCreate() skinPath=$skinPath")

        // 运行时权限申请（6.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (checkSelfPermission(perms[0]) === PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "onCreate() → 请求存储权限")
                requestPermissions(perms, 200)
            }
        }
        // 皮肤状态由 SkinActivity.onPostCreate 自动恢复，无需手动判断
        Log.d(TAG, "onCreate() 完成，等待 onPostCreate 自动换肤...")
    }

    // 换肤按钮（api限制：5.0版本）
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun skinDynamic(view: View) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "=== 用户点击【动态设置主题色】按钮 ===")
        Log.d(TAG, "  当前 SkinManager.currentSkinPath = ${SkinManager.instance?.currentSkinPath}")
        Log.d(TAG, "  目标 skinPath                    = $skinPath")

        // 使用 SkinManager 追踪当前状态，避免重复操作
        if (SkinManager.instance?.currentSkinPath != skinPath) {
            Log.d(TAG, "  → 皮肤状态不同，执行换肤!")
            skinDynamic(skinPath, R.color.skin_item_color)
            PreferencesUtils.putString(this, "currentSkin", "skindemo")
            Log.d(TAG, "  → 已保存 SharedPreferences[currentSkin]=skindemo")
        } else {
            Log.d(TAG, "  → 已是动态皮肤，跳过重复操作")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // 默认按钮（api限制：5.0版本）
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun skinDefault(view: View) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "=== 用户点击【使用宿主默认主题色】按钮 ===")
        Log.d(TAG, "  当前 SkinManager.currentSkinPath = ${SkinManager.instance?.currentSkinPath}")

        // 使用 SkinManager 追踪当前状态，避免重复操作
        if (SkinManager.instance?.currentSkinPath != null) {
            Log.d(TAG, "  → 非默认皮肤，执行恢复默认!")
            defaultSkin(R.color.colorPrimary)
            PreferencesUtils.putString(this, "currentSkin", "default")
            Log.d(TAG, "  → 已保存 SharedPreferences[currentSkin]=default")
        } else {
            Log.d(TAG, "  → 已是默认皮肤，跳过重复操作")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    // 打开 Dialog 测试按钮
    fun openTestDialog(view: View) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "=== 用户点击【打开 Dialog 测试换肤】按钮 ===")
        val dialog = SkinTestDialogFragment.newInstance()
        dialog.show(supportFragmentManager, "SkinTestDialog")
        Log.d(TAG, "  → SkinTestDialogFragment 已显示")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * 系统暗黑模式切换时自动换肤
     * 演示场景：
     * - 切换到暗黑模式 → 自动应用动态皮肤 (skindemo.skin)
     * - 切换到浅色模式 → 自动恢复默认皮肤
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onDarkModeChanged(isDarkMode: Boolean) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "onDarkModeChanged(isDarkMode=$isDarkMode)")
        if (isDarkMode) {
            Log.d(TAG, "  → 暗黑模式：切换到动态皮肤")
            skinDynamic(skinPath, R.color.skin_item_color)
            PreferencesUtils.putString(this, "currentSkin", "skindemo")
        } else {
            Log.d(TAG, "  → 浅色模式：恢复到默认皮肤")
            defaultSkin(R.color.colorPrimary)
            PreferencesUtils.putString(this, "currentSkin", "default")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


}