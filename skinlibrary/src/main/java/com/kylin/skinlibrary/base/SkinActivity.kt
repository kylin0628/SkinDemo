package com.netease.skin.library.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.CustomAppCompatViewInflater
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.utils.ActionBarUtils
import com.kylin.skinlibrary.utils.NavigationUtils
import com.kylin.skinlibrary.utils.StatusBarUtils
import com.kylin.skinlibrary.utils.SystemViewName

/**
 * 换肤Activity父类
 *
 * 内建生命周期感知：onPostCreate 自动应用当前皮肤状态，
 * 子 Activity 只需继承此类，无需手动处理换肤逻辑。
 *
 * 用法：
 * 1、继承此类
 * 2、重写openChangeSkin()方法
 */
abstract class SkinActivity : AppCompatActivity() {
    private var viewInflater: CustomAppCompatViewInflater? = null

    companion object {
        private const val TAG = "[Skin] SkinActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() — ${this.javaClass.simpleName}")
        val layoutInflater = LayoutInflater.from(this)
        LayoutInflaterCompat.setFactory2(layoutInflater, this)
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() — ${this.javaClass.simpleName} Factory2 设置完成")
    }

    /**
     * 在子类 setContentView() 之后自动触发皮肤应用
     * 此时视图树已完整建立，确保换肤生效
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Log.d(TAG, "onPostCreate() — ${this.javaClass.simpleName} | openChangeSkin=${openChangeSkin()}")
        if (openChangeSkin()) {
            Log.d(TAG, "onPostCreate() → 触发自动换肤 applyCurrentSkin()")
            applyCurrentSkin()
        }
    }

    /**
     * 监听系统 Configuration 变化（如暗黑模式切换）
     * 注意：需在 AndroidManifest 中为该 Activity 添加 android:configChanges="uiMode"
     * 否则系统会重建 Activity 而非回调此方法
     *
     * 子类可重写 onDarkModeChanged(isDarkMode: Boolean) 来响应暗黑模式切换
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged() — ${this.javaClass.simpleName}")

        val currentNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> {
                Log.d(TAG, "  → 系统切换到 暗黑模式 (UI_MODE_NIGHT_YES)")
                onDarkModeChanged(true)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                Log.d(TAG, "  → 系统切换到 浅色模式 (UI_MODE_NIGHT_NO)")
                onDarkModeChanged(false)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                Log.d(TAG, "  → 系统模式未定义 (UI_MODE_NIGHT_UNDEFINED)，忽略")
            }
        }
    }

    /**
     * 暗黑模式切换回调
     * 子类重写此方法以实现暗黑/浅色模式下的皮肤自动切换
     *
     * @param isDarkMode true=暗黑模式, false=浅色模式
     */
    protected open fun onDarkModeChanged(isDarkMode: Boolean) {
        Log.d(TAG, "onDarkModeChanged(isDarkMode=$isDarkMode) — 默认空实现，子类可重写")
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        if (openChangeSkin() && !ignoreView(name)) {
            if (viewInflater == null) {
                viewInflater = CustomAppCompatViewInflater(context)
            }
            viewInflater!!.setName(name)
            viewInflater!!.setAttrs(attrs)
            val view = viewInflater!!.autoMatch()
            Log.d(TAG, "onCreateView() → $name → ${view?.javaClass?.simpleName ?: "null"}")
            return view
        }
        return super.onCreateView(parent, name, context, attrs)
    }

    private fun ignoreView(name: String): Boolean {
        when (name) {
            SystemViewName.FRAGMENT_CONTAINER_VIEW, SystemViewName.FRAGMENT -> return true
        }
        return false
    }

    /**
     * @return 是否开启换肤，增加此开关是为了避免开发者误继承此父类，导致未知bug
     */
    protected open fun openChangeSkin(): Boolean {
        return true
    }

    fun defaultSkin(themeColorId: Int) {
        Log.d(TAG, "defaultSkin(themeColorId=$themeColorId) — ${this.javaClass.simpleName}")
        skinDynamic(null, themeColorId)
    }

    /**
     * 动态换肤
     * public 可见性：供 DialogFragment 等外部组件调用
     */
    fun skinDynamic(skinPath: String?, themeColorId: Int) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "skinDynamic() — ${this.javaClass.simpleName}")
        Log.d(TAG, "  skinPath     = $skinPath")
        Log.d(TAG, "  themeColorId = $themeColorId")

        // 统一通过 SkinManager.loadSkin() 管理皮肤状态
        val manager = SkinManager.instance ?: run {
            Log.w(TAG, "skinDynamic → SkinManager 未初始化，跳过")
            return
        }
        manager.loadSkin(skinPath, themeColorId)

        if (themeColorId != 0) {
            val themeColor = manager.getColor(themeColorId)
            Log.d(TAG, "  解析主题色 = #${Integer.toHexString(themeColor)}")
            Log.d(TAG, "  → StatusBar/Navigation/ActionBar 换肤")
            StatusBarUtils.forStatusBar(this, themeColor)
            NavigationUtils.forNavigation(this, themeColor)
            ActionBarUtils.forActionBar(this, themeColor)
        }

        Log.d(TAG, "  → 开始递归遍历 View 树 applyViews(decorView)")
        applyViews(window.decorView)
        Log.d(TAG, "skinDynamic() 完成 — ${this.javaClass.simpleName}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * 自动应用当前皮肤状态
     * 由 onPostCreate 触发，无需 Activity 手动调用
     */
    fun applyCurrentSkin() {
        val currentPath = SkinManager.instance?.currentSkinPath
        val themeColorId = SkinManager.instance?.currentThemeColorId ?: 0
        Log.d(TAG, "applyCurrentSkin() — ${this.javaClass.simpleName}")
        Log.d(TAG, "  当前 skinPath=$currentPath, themeColorId=$themeColorId")
        skinDynamic(currentPath, themeColorId)
    }

    /**
     * 控件回调监听，匹配上则给控件执行换肤方法
     * public 可见性：供 DialogFragment 等外部组件调用，实现跨窗口换肤
     */
    fun applyViews(view: View?) {
        if (view == null) return
        if (view is ViewsMatch) {
            val viewsMatch = view as ViewsMatch
            Log.v(TAG, "applyViews → ${view.javaClass.simpleName}.skinnableView()")
            viewsMatch.skinnableView()
        }
        if (view is ViewGroup) {
            val parent = view
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                applyViews(parent.getChildAt(i))
            }
        }
    }
}