package com.netease.skin.library.base

import android.R
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.withStyledAttributes
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.SkinUiHost
import com.kylin.skinlibrary.SkinnableResources
import com.kylin.skinlibrary.SkinnableThemeHost
import com.kylin.skinlibrary.core.CustomAppCompatViewInflater
import com.kylin.skinlibrary.utils.ActionBarUtils
import com.kylin.skinlibrary.utils.NavigationUtils
import com.kylin.skinlibrary.utils.SkinLog
import com.kylin.skinlibrary.utils.StatusBarUtils
import com.kylin.skinlibrary.utils.SystemViewName
import com.netease.skin.library.core.ViewsMatch

/**
 * [SkinActivity] 的组合（非继承）等价物。
 *
 * 适用于「已有 BaseActivity / 无法换基类继承 [SkinActivity]」的宿主：在自己 Activity 里
 * 持有一个本 delegate 实例，把少量生命周期与 `onCreateView` / `getResources` 转发过来即可
 * 获得与继承 [SkinActivity] 相同的换肤能力。转发样板无法完全消除（AppCompat 在 onCreate
 * 已占用自身 Factory2，只能经覆写 onCreateView 转发），但换肤逻辑本体都在这里、业务侧零重复。
 *
 * 典型转发（宿主 Activity 内）：
 * ```
 * class MyActivity : AppCompatActivity() {
 *     private val skin = SkinActivityDelegate(this)
 *     override fun getResources(): Resources = skin.resources(super.getResources())
 *     override fun onCreateView(p, n, c, a) = skin.createView(p, n, c, a) ?: super.onCreateView(p, n, c, a)
 *     override fun onPostCreate(b) { super.onPostCreate(b); skin.onPostCreate() }
 *     override fun onResume() { super.onResume(); skin.onResume() }
 *     override fun onConfigurationChanged(c) { super.onConfigurationChanged(c); skin.onConfigurationChanged(c) }
 *     fun skinDynamic(path: String?) = skin.skinDynamic(path)
 * }
 * ```
 */
class SkinActivityDelegate(
    private val activity: AppCompatActivity,
    private val openChangeSkin: () -> Boolean = { true }
) : SkinnableThemeHost {

    private var viewInflater: CustomAppCompatViewInflater? = null

    /** 皮肤感知的 Resources 缓存（默认皮肤返回 null → 走 super.getResources()） */
    private var skinnableResources: SkinnableResources? = null

    /** 首帧 onResume 跳过兜底：onPostCreate 已做过一次 applyCurrentSkin()，避免重复刷 */
    private var firstResumeSkipped = false

    /** 上次已应用（完成遍历）的 skinVersion，用于 onResume 兜底跳过无谓全量重刷。 */
    private var lastAppliedSkinVersion = -1

    /** 实例级换肤工厂注册表：一个 Factory 只作用于当前 Activity，实例销毁即回收 */
    private val skinnableViewFactories = java.util.concurrent.CopyOnWriteArrayList<SkinnableViewFactory>()

    /**
     * 皮肤感知的 [Resources]：宿主 Activity 的 `getResources()` 应返回本方法结果，
     * 使 Compose 的 `colorResource`/`stringResource`/`dimensionResource` 按皮肤包同名资源取值。
     */
    fun resources(hostResources: Resources): Resources {
        val manager = SkinManager.instance
        if (manager == null) return hostResources
        var res = skinnableResources
        if (res == null) {
            res = SkinnableResources(hostResources)
            skinnableResources = res
        }
        return res
    }

    /**
     * 换肤控件工厂拦截：宿主 Activity 覆写 `onCreateView(parent, name, context, attrs)` 时调用，
     * 返回非 null 即接管该控件；返回 null 表示不关心，交 `super.onCreateView(...)`。
     */
    fun createView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        // 1) 第三方 Factory 责任链：始终最先尝试，实现多库经 Factory 拦截控件的兼用。
        for (factory in LayoutFactoryRegistry.snapshot()) {
            val view = try {
                factory.onCreateView(parent, name, context, attrs)
            } catch (e: Exception) {
                SkinLog.e(TAG, "注册 Factory 拦截 $name 异常，已跳过: ${factory.javaClass.name}", e)
                null
            }
            if (view != null) {
                (view as? ViewsMatch)?.skinnableView()
                SkinLog.d(TAG, "createView() → $name → 命中第三方 Factory ${factory.javaClass.simpleName}")
                return view
            }
        }

        // 2) 主题库自身换肤匹配（受 openChangeSkin + ignoreView 门控）
        if (openChangeSkin() && !ignoreView(name)) {
            val view = createSkinnableView(name, context, attrs)
            (view as? ViewsMatch)?.skinnableView()
            SkinLog.d(TAG, "createView() → $name → ${view?.javaClass?.simpleName ?: "null"}")
            return view
        }
        // 3) 交宿主 super.onCreateView 兜底（Fragment / openChangeSkin 关闭等）
        return null
    }

    private fun ignoreView(name: String): Boolean =
        name == SystemViewName.FRAGMENT_CONTAINER_VIEW || name == SystemViewName.FRAGMENT

    private fun createSkinnableView(name: String, context: Context, attrs: AttributeSet): View? {
        for (factory in skinnableViewFactories.toList().asReversed()) {
            val view = try {
                factory(name, context, attrs)
            } catch (e: Exception) {
                SkinLog.e(TAG, "注册的 SkinnableViewFactory 拦截 $name 异常，已跳过", e)
                null
            }
            if (view != null) return view
        }
        if (viewInflater == null) viewInflater = CustomAppCompatViewInflater(context)
        viewInflater!!.setName(name)
        viewInflater!!.setAttrs(attrs)
        return viewInflater!!.autoMatch()
    }

    /** 宿主 `onPostCreate` 里调用，自动应用当前皮肤状态。 */
    fun onPostCreate() {
        SkinLog.d(TAG, "onPostCreate() — ${activity.javaClass.simpleName} | openChangeSkin=${openChangeSkin()}")
        if (openChangeSkin()) {
            SkinLog.d(TAG, "onPostCreate() → 触发自动换肤 applyCurrentSkin()")
            applyCurrentSkin()
        }
    }

    /** 宿主 `onResume` 里调用（在 super.onResume() 之后）。 */
    fun onResume() {
        if (!firstResumeSkipped) {
            firstResumeSkipped = true
            return
        }
        if (openChangeSkin() && SkinManager.instance?.skinVersion != lastAppliedSkinVersion) {
            SkinLog.d(TAG, "onResume() — ${activity.javaClass.simpleName} 兜底 applyCurrentSkin()")
            applyCurrentSkin()
        }
    }

    /** 宿主 `onConfigurationChanged` 里调用（在 super 之后）。 */
    fun onConfigurationChanged(newConfig: Configuration) {
        SkinLog.d(TAG, "onConfigurationChanged() — ${activity.javaClass.simpleName}")
        val currentNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> onDarkModeChanged(true)
            Configuration.UI_MODE_NIGHT_NO -> onDarkModeChanged(false)
            Configuration.UI_MODE_NIGHT_UNDEFINED ->
                SkinLog.d(TAG, "  → 系统模式未定义 (UI_MODE_NIGHT_UNDEFINED)，忽略")
        }
    }

    private fun onDarkModeChanged(isDarkMode: Boolean) {
        SkinUiHost.applyTheme?.invoke(this, isDarkMode, false)
    }

    override fun defaultSkin() {
        SkinLog.d(TAG, "defaultSkin() — ${activity.javaClass.simpleName}")
        skinDynamic(null)
    }

    override fun skinDynamic(skinPath: String?) {
        SkinLog.i(TAG, "skinDynamic() → ${activity.javaClass.simpleName} | skinPath=$skinPath")
        val manager = SkinManager.instance ?: run {
            SkinLog.w(TAG, "skinDynamic → SkinManager 未初始化，跳过")
            return
        }
        manager.loadSkin(skinPath)
        lastAppliedSkinVersion = manager.skinVersion

        val themeColor = resolveThemeColor()
        if (themeColor != 0) {
            SkinLog.d(TAG, "解析主题色 = #${Integer.toHexString(themeColor)} → StatusBar/Navigation/ActionBar 换肤")
            StatusBarUtils.forStatusBar(activity, themeColor)
            NavigationUtils.forNavigation(activity, themeColor)
            ActionBarUtils.forActionBar(activity, themeColor)
        }

        SkinLog.d(TAG, "开始遍历 View 树 applyViews(decorView) + 弹框")
        applyViews(activity.window.decorView)
        applyViewsToDialogs()
        SkinLog.i(TAG, "skinDynamic() 完成 → ${activity.javaClass.simpleName}")
    }

    fun applyCurrentSkin() {
        val currentPath = SkinManager.instance?.currentSkinPath
        SkinLog.d(TAG, "applyCurrentSkin() — ${activity.javaClass.simpleName}")
        SkinLog.d(TAG, "  当前 skinPath=$currentPath")
        skinDynamic(currentPath)
    }

    /** 以列表注册 [SkinnableViewFactory]，需在 setContentView 触发 inflate 之前调用。 */
    fun registerSkinnableViewFactories(factories: List<SkinnableViewFactory>) {
        skinnableViewFactories.addAll(factories)
    }

    /** 以「控件类 + 标签名」绑定列表注册第三方换肤控件。 */
    fun registerSkinnableViews(binders: List<SkinnableViewBinder>) {
        skinnableViewFactories.addAll(
            binders.map { binder ->
                { name: String, context: Context, attrs: AttributeSet ->
                    if (name in binder.names) binder.creator(context, attrs) else null
                }
            }
        )
    }

    private fun resolveThemeColor(): Int {
        val manager = SkinManager.instance ?: return 0
        var themeColorId = 0
        activity.withStyledAttributes(
            attrs = intArrayOf(R.attr.colorAccent, R.attr.colorPrimary, R.attr.statusBarColor)
        ) {
            themeColorId = getResourceId(0, 0)
                .takeIf { it != 0 }
                ?: getResourceId(1, 0).takeIf { it != 0 }
                ?: getResourceId(2, 0)
        }
        if (themeColorId == 0) {
            SkinLog.d(TAG, "resolveThemeColor → 无 colorAccent/colorPrimary/statusBarColor 资源，跳过着色")
            return 0
        }
        return manager.getColor(themeColorId)
    }

    private fun applyViewsToDialogs() {
        applyViewsToDialogs(activity.supportFragmentManager)
    }

    private fun applyViewsToDialogs(fragmentManager: androidx.fragment.app.FragmentManager) {
        for (fragment in fragmentManager.fragments) {
            val dialogFragment = fragment as? androidx.fragment.app.DialogFragment
            dialogFragment?.dialog?.window?.decorView?.let { decorView ->
                SkinLog.d(TAG, "applyViewsToDialogs() → ${dialogFragment.javaClass.simpleName}.decorView")
                applyViews(decorView)
            }
            applyViewsToDialogs(fragment.childFragmentManager)
        }
    }

    fun applyViews(view: View?) {
        if (view == null) return
        if (view is ViewsMatch) {
            view.skinnableView()
        }
        if (view is ViewGroup) {
            val childCount = view.childCount
            for (i in 0 until childCount) {
                applyViews(view.getChildAt(i))
            }
        }
    }

    private companion object {
        const val TAG = "SkinActivityDelegate"
    }
}
