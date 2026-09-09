package com.kylin.skindemo

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
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

        // 1.6 注册「应用一套主题」统一策略：isDark=true → 深色/动态皮肤，false → 浅色/默认皮肤。
        //     两处复用同一实现，保证「app 内切换」与「跟随系统变化」行为一致：
        //      - SkinActivity.onDarkModeChanged 转发系统深浅色变化；
        //      - 宿主切肤入口（悬浮按钮弹框）在用户点选「默认/动态主题」时调用（切肤同时切深浅色）。
        //     皮肤包路径只在宿主维护，第三方页面无需引用 app 模块资源。
        SkinUiHost.applyTheme = { activity, isDark, forceNightMode ->
            Log.d(TAG, "应用主题: isDark=$isDark, forceNightMode=$forceNightMode → ${activity.javaClass.simpleName}")

            // 回声抑制：用户主动切换（forceNightMode=true）会 setDefaultNightMode(YES/NO)，
            // 随后触发 onConfigurationChanged → onDarkModeChanged 以 forceNightMode=false 转发回来。
            // 若此时仍把夜间模式重置为 FOLLOW_SYSTEM，会撤销用户刚做的主动选择——
            // 点「动态主题」被跟随系统的回声一步步撤销，最终落回默认皮肤（自动弹回）。
            // 故当 defaultNightMode 已是强制 YES/NO 时，判定本次为主动切换触发的回声，直接吸收：
            // 既不重置夜间模式，也不重复换肤（主动分支已完成换肤）。
            val currentNightMode = AppCompatDelegate.getDefaultNightMode()
            val isEchoOfForcedMode = !forceNightMode &&
                (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES ||
                    currentNightMode == AppCompatDelegate.MODE_NIGHT_NO)

            if (!isEchoOfForcedMode) {
                // 1) 同步 AppCompat 夜间模式：BYD 弹框/控件按 uiMode 取色（DayNight token），
                //    只换皮肤不切夜间模式时它们不跟随。
                //    - 用户主动切换（forceNightMode=true）：强制 YES/NO，让 uiMode 立即随主题切换；
                //    - 跟随系统（forceNightMode=false）：重置为 FOLLOW_SYSTEM，避免污染全局默认模式，
                //      导致无 configChanges=uiMode 的页面（比亚迪演示页）重建时卡在错误深浅色。
                AppCompatDelegate.setDefaultNightMode(
                    when {
                        !forceNightMode -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        isDark -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_NO
                    }
                )
                // 2) 换肤：深色 → 动态皮肤，浅色 → 默认皮肤。
                if (isDark) {
                    val path =
                        "${getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"
                    activity.skinDynamic(path)
                    PreferencesUtils.putString(activity, "currentSkin", "skindemo")
                } else {
                    activity.defaultSkin()
                    PreferencesUtils.putString(activity, "currentSkin", "default")
                }
            } else {
                Log.d(TAG, "  忽略主动强制模式(defaultNightMode=$currentNightMode)触发的回声回调，保持用户选择")
            }
        }

        // 2. 跟随系统深浅色模式（AppCompat 原生 DayNight）。
        //    修复「系统切深色后 app 主题不跟随」：默认模式下 AppCompatDelegate 会强制跟随系统，
        //    但换肤库覆盖了 getResources() 且 Activity 声明 configChanges=uiMode 后重建链路被短路，
        //    故在此显式对齐系统，保证 DayNight（含 BYD DayNight token）能随系统变化。
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // 3. 从持久化存储恢复上次的皮肤状态，注入 SkinManager
        restoreSkinState()

        // 4. 拷贝皮肤包资源
        try {
            Log.d(TAG, "步骤4: 拷贝皮肤包资源到外部存储")
            AssetsUtils.doCopy(
                this,
                "skin",
                "${applicationContext.getExternalFilesDir("skindemo")!!.absolutePath}"
            )
            Log.d(TAG, "步骤4: 皮肤包资源拷贝完成")
        } catch (e: IOException) {
            Log.e(TAG, "步骤4: 拷贝皮肤包资源失败!", e)
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
        Log.d(TAG, "步骤3: 恢复皮肤状态")
        val currentSkin = PreferencesUtils.getString(this, "currentSkin")
        Log.d(TAG, "步骤3: SharedPreferences[currentSkin]=$currentSkin")

        if ("skindemo" == currentSkin) {
            val skinPath =
                "${getExternalFilesDir("skindemo")!!.absolutePath}${File.separator}skindemo.skin"
            Log.d(TAG, "步骤3: 上次为动态皮肤 → skinPath=$skinPath（对齐深色模式）")
            // 动态皮肤与深色模式绑定：冷启动恢复时同步夜间模式，避免「深色皮肤 + 浅色 uiMode」
            // 导致 BYD 弹框/控件仍取浅色 token、与页面皮肤不一致。
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            SkinManager.instance?.loadSkin(skinPath)
        } else {
            Log.d(TAG, "步骤3: 上次为默认皮肤（或首次启动）→ 保持跟随系统")
            // 默认皮肤不强制浅色：保持 FOLLOW_SYSTEM，让「跟随系统变化」在首次启动时即生效。
            SkinManager.instance?.loadSkin(null)
        }
        Log.d(TAG, "步骤3: 皮肤状态恢复完成 → currentSkinPath=${SkinManager.instance?.currentSkinPath}")
    }
}