package com.kylin.skindemo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.SkinUiHost
import com.kylin.skinlibrary.SkinnableThemeHost
import com.netease.skin.library.base.SkinDialog

/**
 * 全局主题切换器。
 *
 * 提供悬浮切肤入口 + 主题切换弹框。关键设计：**每个独立 Window 都注入悬浮按钮**。
 *
 * Dialog / PopupWindow / DialogFragment 都拥有独立 Window，会盖在 Activity Window 之上，
 * Activity content 区的悬浮按钮会被遮挡。因此除 Activity 外，每个弹框显示时都向其
 * `window.decorView`（或 PopupWindow 的 contentView 容器）再注入一个悬浮按钮，
 * 实现「单页 + 多层弹框」任意层级都能切肤看效果。
 */
object ThemeSwitcher {

    private const val TAG_FAB = "theme_switcher_fab"

    /** 在 Activity 的 content 区安装悬浮按钮（幂等） */
    fun installFab(activity: Activity) {
        val content = activity.window.decorView
            .findViewById<ViewGroup>(android.R.id.content) ?: return
        installFabInto(content, activity)
    }

    /**
     * 给任意 [ViewGroup]（需为 FrameLayout 系，如 Dialog 的 decorView）注入悬浮按钮。
     * 幂等：同一容器重复调用不叠加。
     */
    fun installFabInto(host: ViewGroup, context: Context) {
        if (host !is FrameLayout) return
        if (host.findViewWithTag<View>(TAG_FAB) != null) return
        val activity = findActivity(context) ?: return
        val fab = createFab(context) { show(activity) }
        host.addView(fab)
        refreshFab(fab)
        // 切肤时同步刷新悬浮按钮图标/背景色（detached 后跳过，避免对已销毁窗口无效刷新）
        SkinManager.instance?.addSkinChangeListener {
            if (fab.isAttachedToWindow) refreshFab(fab)
        }
    }

    /**
     * 给 PopupWindow 注入悬浮按钮：PopupWindow 无 decorView 暴露，且 contentView 可能非
     * FrameLayout，故把它包进一层 [FrameLayout] 再挂悬浮按钮。
     */
    fun installFabIntoPopup(popup: PopupWindow, context: Context) {
        val original = popup.contentView ?: return
        if (original.findViewWithTag<View>(TAG_FAB) != null) return
        val container = FrameLayout(context)
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        container.addView(original, lp)
        popup.contentView = container
        installFabInto(container, context)
    }

    /** 弹出全局主题切换弹框 */
    fun show(activity: Activity) {
        ThemeSwitcherDialog(activity).showWithSkin()
    }

    private fun createFab(context: Context, onClick: () -> Unit): FloatingActionButton {
        return FloatingActionButton(context).apply {
            tag = TAG_FAB
            val size = (56 * resources.displayMetrics.density).toInt()
            val margin = (16 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, margin, margin)
            }
            contentDescription = "全局主题切换"
            setOnClickListener { onClick() }
        }
    }

    private fun refreshFab(fab: FloatingActionButton) {
        val manager = SkinManager.instance ?: return
        fab.setImageDrawable(manager.getDrawableOrMipMap(R.drawable.ic_skin_demo))
        fab.backgroundTintList = ColorStateList.valueOf(manager.getColor(R.color.main_style))
    }

    /**
     * 从任意 Context（Dialog 的 ContextThemeWrapper / 弹框 context）递归解包出宿主 [Activity]。
     * 修复「原生 Dialog 里 context as? Activity 失效」：Dialog 构造会包一层 ContextThemeWrapper，
     * 直接强转拿不到 Activity，导致弹框内切换按钮静默失效。
     */
    fun findActivity(context: Context?): Activity? {
        var ctx = context
        while (ctx != null) {
            if (ctx is Activity) return ctx
            ctx = if (ctx is ContextWrapper) ctx.baseContext else null
        }
        return null
    }

    /** 保留兼容别名：老调用点（SkinActivity 场景）仍可用 */
    fun findSkinActivity(context: Context?): SkinnableThemeHost? = findActivity(context) as? SkinnableThemeHost
}

/**
 * 全局主题切换弹框（原生 Dialog）。
 *
 * 拥有独立 Window，自行设置 Factory2 创建 Skinnable* 控件，并注册到 [SkinManager]，
 * 使切肤时弹框自身也能跟随刷新。
 */
class ThemeSwitcherDialog(context: Context) : SkinDialog(context) {

    private val activity: Activity?
        get() = ThemeSwitcher.findActivity(context)

    override fun getLayoutResId(): Int = R.layout.dialog_theme_switcher

    override fun onContentViewCreated(root: View) {
        updateStatus(root)

        // 弹框内也注入悬浮切肤入口（本弹框是独立 Window，Activity 的悬浮按钮被遮挡）
        (window?.decorView as? FrameLayout)?.let { ThemeSwitcher.installFabInto(it, context) }

        root.findViewById<View>(R.id.btn_theme_default)?.setOnClickListener {
            applySkin(null, "default", root)
        }
        root.findViewById<View>(R.id.btn_theme_dynamic)?.setOnClickListener {
            val skinPath = "${context.applicationContext.getExternalFilesDir("skindemo")?.absolutePath}/skindemo.skin"
            applySkin(skinPath, "skindemo", root)
        }
    }

    /**
     * 统一切肤入口：宿主为 [SkinnableThemeHost]（继承版 [SkinActivity] 或组合版
     * [SkinActivityDelegate]）走 [SkinUiHost.applyTheme] 统一策略（切夜间模式 + 换肤），
     * 与「跟随系统变化」共用同一条链路，保证 BYD 弹框/控件（按 uiMode 取色）在 app 内切换
     * 时同样跟随；未注册钩子时回落直接换肤。普通 Activity 直接 loadSkin。
     */
    private fun applySkin(skinPath: String?, prefValue: String, root: View) {
        when (val act = activity as? SkinnableThemeHost) {
            is SkinnableThemeHost -> {
                // 动态皮肤 → 深色模式，默认皮肤 → 浅色模式；与跟随系统共用 applyTheme 统一策略。
                val applied = SkinUiHost.applyTheme?.let { hook ->
                    hook(act, skinPath != null, true)
                    true
                } ?: false
                if (!applied) {
                    if (skinPath == null) act.defaultSkin() else act.skinDynamic(skinPath)
                }
            }
            else -> {
                SkinManager.instance?.loadSkin(skinPath)
            }
        }
        SkinApp.persistCurrentSkin(context, prefValue)
        // 弹框自身内容（Skinnable* 控件）按新皮肤重刷
        SkinManager.instance?.applySkin(root)
        updateStatus(root)
    }

    private fun updateStatus(root: View) {
        val isDefault = SkinManager.instance?.currentSkinPath == null
        root.findViewById<android.widget.TextView>(R.id.tv_theme_switcher_status)?.text =
            if (isDefault) "当前：默认皮肤" else "当前：动态皮肤 (skindemo.skin)"
    }
}
