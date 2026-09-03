package com.netease.skin.library.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.AttributeSet
import com.kylin.skinlibrary.utils.SkinLog
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.SkinnableResources
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

    /** 皮肤感知的 Resources 缓存（默认皮肤返回 null → 走 super.getResources()） */
    private var skinnableResources: SkinnableResources? = null

    /** 首帧 onResume 跳过兜底：onPostCreate 已做过一次 applyCurrentSkin()，避免重复刷 */
    private var firstResumeSkipped = false

    companion object {
        private const val TAG = "SkinActivity"
    }

    /**
     * 皮肤感知的 [Resources]：暗色皮肤时返回 [SkinnableResources]，使 Compose 的
     * `colorResource`/`painterResource`/`stringResource`/`dimensionResource`（内部都是
     * `LocalContext.current.resources.getXxx(id)`）自动按皮肤包同名资源取值。
     *
     * 关键：这里只换 `getResources()`，不换 `LocalContext` 本身，故
     * `LocalContext.current as ComponentActivity` 之类强转不受影响（LocalContext 仍是 Activity）。
     * 默认皮肤返回 `super.getResources()`，行为与原生完全一致。
     */
    override fun getResources(): Resources {
        val manager = SkinManager.instance
        if (manager == null || manager.isDefaultSkin) {
            return super.getResources()
        }
        var res = skinnableResources
        if (res == null) {
            res = SkinnableResources(super.getResources())
            skinnableResources = res
        }
        return res
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SkinLog.d(TAG, "onCreate() — ${this.javaClass.simpleName}")
        val layoutInflater = LayoutInflater.from(this)
        LayoutInflaterCompat.setFactory2(layoutInflater, this)
        super.onCreate(savedInstanceState)
        SkinLog.d(TAG, "onCreate() — ${this.javaClass.simpleName} Factory2 设置完成")
    }

    /**
     * 在子类 setContentView() 之后自动触发皮肤应用
     * 此时视图树已完整建立，确保换肤生效
     */
    override fun onResume() {
        super.onResume()
        // 首帧 onResume 紧跟 onPostCreate（已做过 applyCurrentSkin），跳过避免重复。
        if (!firstResumeSkipped) {
            firstResumeSkipped = true
            return
        }
        // 跨页切肤一致性兜底：在第三方页面（如比亚迪演示页）切肤后返回本页时，
        // 本页 Skinnable* 控件不会自动重刷，这里按当前皮肤重刷一遍。
        if (openChangeSkin()) {
            SkinLog.d(TAG, "onResume() — ${this.javaClass.simpleName} 兜底 applyCurrentSkin()")
            applyCurrentSkin()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        SkinLog.d(TAG, "onPostCreate() — ${this.javaClass.simpleName} | openChangeSkin=${openChangeSkin()}")
        if (openChangeSkin()) {
            SkinLog.d(TAG, "onPostCreate() → 触发自动换肤 applyCurrentSkin()")
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
        SkinLog.d(TAG, "onConfigurationChanged() — ${this.javaClass.simpleName}")

        val currentNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> {
                SkinLog.d(TAG, "  → 系统切换到 暗黑模式 (UI_MODE_NIGHT_YES)")
                onDarkModeChanged(true)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                SkinLog.d(TAG, "  → 系统切换到 浅色模式 (UI_MODE_NIGHT_NO)")
                onDarkModeChanged(false)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                SkinLog.d(TAG, "  → 系统模式未定义 (UI_MODE_NIGHT_UNDEFINED)，忽略")
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
        SkinLog.d(TAG, "onDarkModeChanged(isDarkMode=$isDarkMode) — 默认空实现，子类可重写")
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        // 1) 第三方 Factory 责任链：始终最先尝试，实现多库经 Factory 拦截控件的兼用。
        //    约定：每个 Factory 对不关心的 View 必须返回 null 放行，谁命中谁返回。
        for (factory in LayoutFactoryRegistry.snapshot()) {
            val view = try {
                factory.onCreateView(parent, name, context, attrs)
            } catch (e: Exception) {
                // 单个第三方 Factory 异常不应拖垮整条链，跳过并留痕，交下一个节点
                SkinLog.e(TAG, "注册 Factory 拦截 $name 异常，已跳过: ${factory.javaClass.name}", e)
                null
            }
            if (view != null) {
                // 第三方返回的 View 若也实现 ViewsMatch，补一次换肤初始化
                (view as? ViewsMatch)?.skinnableView()
                SkinLog.d(TAG, "onCreateView() → $name → 命中第三方 Factory ${factory.javaClass.simpleName}")
                return view
            }
        }

        // 2) 主题库自身换肤匹配（受 openChangeSkin + ignoreView 门控）
        if (openChangeSkin() && !ignoreView(name)) {
            val view = createSkinnableView(name, context, attrs)
            // 自换肤:视图 inflate 即刻按当前皮肤换肤,覆盖 onPostCreate 之后才创建的视图
            // (RecyclerView 列表项 / ViewPager Fragment 内容 / ViewStub / 弹框内容)——
            // 它们被 Factory2 包成 Skinnable* 并记录属性,但等不到 applyViews 的一次性遍历。
            (view as? ViewsMatch)?.skinnableView()
            SkinLog.d(TAG, "onCreateView() → $name → ${view?.javaClass?.simpleName ?: "null"}")
            return view
        }
        // 3) AppCompat 兜底（Fragment/FragmentContainerView，或 openChangeSkin 关闭时）
        return super.onCreateView(parent, name, context, attrs)
    }

    private fun ignoreView(name: String): Boolean {
        when (name) {
            SystemViewName.FRAGMENT_CONTAINER_VIEW, SystemViewName.FRAGMENT -> return true
        }
        return false
    }

    /**
     * 换肤控件工厂钩子：把 XML 标签名替换成实现 [ViewsMatch] 的换肤控件。
     *
     * 默认走 [CustomAppCompatViewInflater]（原生/AppCompat/Material 控件 → Skinnable*）。
     * 子类可重写以替换成第三方控件（如比亚迪页用 SkinnableByd*），使其进入主题库统一的
     * `applyViews → skinnableView()` 换肤遍历，而非另起桥接。重写方对「不关心的控件」
     * 应回落 `super.createSkinnableView(...)` 或返回 null 放行。
     *
     * @return 返回 null 表示本 Activity 不拦截该控件（交给 AppCompat 兜底创建普通控件）。
     */
    protected open fun createSkinnableView(name: String, context: Context, attrs: AttributeSet): View? {
        if (viewInflater == null) {
            viewInflater = CustomAppCompatViewInflater(context)
        }
        viewInflater!!.setName(name)
        viewInflater!!.setAttrs(attrs)
        return viewInflater!!.autoMatch()
    }

    /**
     * @return 是否开启换肤，增加此开关是为了避免开发者误继承此父类，导致未知bug
     */
    protected open fun openChangeSkin(): Boolean {
        return true
    }

    fun defaultSkin(themeColorId: Int) {
        SkinLog.d(TAG, "defaultSkin(themeColorId=$themeColorId) — ${this.javaClass.simpleName}")
        skinDynamic(null, themeColorId)
    }

    /**
     * 动态换肤
     * public 可见性：供 DialogFragment 等外部组件调用
     */
    fun skinDynamic(skinPath: String?, themeColorId: Int) {
        SkinLog.i(TAG, "skinDynamic() → ${this.javaClass.simpleName} | skinPath=$skinPath, themeColorId=$themeColorId")

        // 统一通过 SkinManager.loadSkin() 管理皮肤状态
        val manager = SkinManager.instance ?: run {
            SkinLog.w(TAG, "skinDynamic → SkinManager 未初始化，跳过")
            return
        }
        manager.loadSkin(skinPath, themeColorId)

        if (themeColorId != 0) {
            val themeColor = manager.getColor(themeColorId)
            SkinLog.d(TAG, "解析主题色 = #${Integer.toHexString(themeColor)} → StatusBar/Navigation/ActionBar 换肤")
            StatusBarUtils.forStatusBar(this, themeColor)
            NavigationUtils.forNavigation(this, themeColor)
            ActionBarUtils.forActionBar(this, themeColor)
        }

        SkinLog.d(TAG, "开始遍历 View 树 applyViews(decorView) + 弹框")
        applyViews(window.decorView)
        // 弹框跟随:遍历已打开的 DialogFragment,对其 window.decorView 换肤。
        // 弹框窗口独立于 Activity 的 decorView,切主题时不会被 applyViews(window.decorView) 覆盖。
        applyViewsToDialogs()
        SkinLog.i(TAG, "skinDynamic() 完成 → ${this.javaClass.simpleName}")
    }

    /**
     * 自动应用当前皮肤状态
     * 由 onPostCreate 触发，无需 Activity 手动调用
     */
    fun applyCurrentSkin() {
        val currentPath = SkinManager.instance?.currentSkinPath
        val themeColorId = SkinManager.instance?.currentThemeColorId ?: 0
        SkinLog.d(TAG, "applyCurrentSkin() — ${this.javaClass.simpleName}")
        SkinLog.d(TAG, "  当前 skinPath=$currentPath, themeColorId=$themeColorId")
        skinDynamic(currentPath, themeColorId)
    }

    /**
     * 遍历当前已打开的 DialogFragment，对其 window.decorView 换肤。
     * DialogFragment 拥有独立 Window，内容不受 applyViews(window.decorView) 覆盖，
     * 故切主题时需单独遍历，使已打开弹框跟随皮肤变化。
     *
     * 递归遍历所有 FragmentManager 层级：弹框可能挂在 childFragmentManager（如
     * Fragment 内部 `DialogFragment().show(childFragmentManager, ...)`），只遍历顶层
     * supportFragmentManager 会漏掉这类弹框，导致切主题时它们不跟随换肤。
     */
    private fun applyViewsToDialogs() {
        applyViewsToDialogs(supportFragmentManager)
    }

    private fun applyViewsToDialogs(fragmentManager: androidx.fragment.app.FragmentManager) {
        for (fragment in fragmentManager.fragments) {
            val dialogFragment = fragment as? androidx.fragment.app.DialogFragment
            dialogFragment?.dialog?.window?.decorView?.let { decorView ->
                SkinLog.d(TAG, "applyViewsToDialogs() → ${dialogFragment.javaClass.simpleName}.decorView")
                applyViews(decorView)
            }
            // 递归子 FragmentManager（子 Fragment 或子 Fragment 内弹框）
            applyViewsToDialogs(fragment.childFragmentManager)
        }
    }

    /**
     * 控件回调监听，匹配上则给控件执行换肤方法
     * public 可见性：供 DialogFragment 等外部组件调用，实现跨窗口换肤
     */
    fun applyViews(view: View?) {
        if (view == null) return
        if (view is ViewsMatch) {
            val viewsMatch = view as ViewsMatch
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