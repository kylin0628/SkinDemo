package com.kylin.skindemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
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
        skinPath = "${applicationContext.getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"
        Log.d(TAG, "onCreate() skinPath=$skinPath")

        // 存储权限（API < 33 需要）
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            Log.d(TAG, "onCreate() → 请求存储权限")
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 200)
        }
        // 皮肤状态由 SkinActivity.onPostCreate 自动恢复，无需手动判断
        Log.d(TAG, "onCreate() 完成，等待 onPostCreate 自动换肤...")
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 全局主题切换悬浮按钮：右下角入口，任何页面都能切肤看效果
        ThemeSwitcher.installFab(this)
    }

    /** 切换动态主题按钮 */
    fun skinDynamic(view: View) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "=== 用户点击【动态设置主题色】按钮 ===")
        Log.d(TAG, "  当前 SkinManager.currentSkinPath = ${SkinManager.instance?.currentSkinPath}")
        Log.d(TAG, "  目标 skinPath                    = $skinPath")

        if (SkinManager.instance?.currentSkinPath != skinPath) {
            Log.d(TAG, "  → 皮肤状态不同，执行换肤!")
            skinDynamic(skinPath, R.color.skin_item_color)
            PreferencesUtils.putString(this, "currentSkin", "skindemo")
        } else {
            Log.d(TAG, "  → 已是动态皮肤，跳过重复操作")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /** 切换默认主题按钮 */
    fun skinDefault(view: View) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "=== 用户点击【使用宿主默认主题色】按钮 ===")
        Log.d(TAG, "  当前 SkinManager.currentSkinPath = ${SkinManager.instance?.currentSkinPath}")

        if (SkinManager.instance?.currentSkinPath != null) {
            Log.d(TAG, "  → 非默认皮肤，执行恢复默认!")
            defaultSkin(R.color.colorPrimary)
            PreferencesUtils.putString(this, "currentSkin", "default")
        } else {
            Log.d(TAG, "  → 已是默认皮肤，跳过重复操作")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /** 打开 Dialog 测试 */
    fun openTestDialog(view: View) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "=== 用户点击【打开 Dialog 测试换肤】按钮 ===")
        SkinTestDialogFragment.newInstance().show(supportFragmentManager, "SkinTestDialog")
        Log.d(TAG, "  → SkinTestDialogFragment 已显示")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /** 打开 Compose 组件案例页 */
    fun openComposeDemo(view: View) {
        Log.d(TAG, "=== 打开 Compose 组件案例页 ===")
        startActivity(android.content.Intent(this, ComposeDemoActivity::class.java))
    }

    /** 打开比亚迪官方控件演示页 */
    fun openBydWidgetDemo(view: View) {
        Log.d(TAG, "=== 打开比亚迪官方控件演示页 ===")
        startActivity(android.content.Intent(this, com.kylin.bydwidget.BydWidgetDemoActivity::class.java))
    }

    /** 弹 PopupWindow */
    fun openPopupWindow(view: View) {
        Log.d(TAG, "=== 弹 PopupWindow ===")
        PopupWindowDemo.show(this, view)
    }

    /** 弹原生 Dialog */
    fun openNativeDialog(view: View) {
        Log.d(TAG, "=== 弹原生 Dialog ===")
        SkinTestDialog(this).showWithSkin()
    }

    /** 弹多层弹框（列表） */
    fun openMultiLevelDialog(view: View) {
        Log.d(TAG, "=== 弹多层弹框（列表） ===")
        MultiLevelDialogFragment.newInstance(1).show(supportFragmentManager, "MultiLevel_1")
    }

    /** 暗黑模式 → 动态皮肤 / 浅色模式 → 默认皮肤 */
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
