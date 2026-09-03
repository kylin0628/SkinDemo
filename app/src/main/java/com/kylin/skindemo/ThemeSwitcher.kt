package com.kylin.skindemo

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.core.view.LayoutInflaterCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.CustomAppCompatViewInflater
import com.kylin.skinlibrary.utils.PreferencesUtils
import com.netease.skin.library.base.SkinActivity

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
    fun findSkinActivity(context: Context?): SkinActivity? = findActivity(context) as? SkinActivity
}

/**
 * 全局主题切换弹框（原生 Dialog）。
 *
 * 拥有独立 Window，自行设置 Factory2 创建 Skinnable* 控件，并注册到 [SkinManager]，
 * 使切肤时弹框自身也能跟随刷新。
 */
class ThemeSwitcherDialog(context: Context) : Dialog(context), LayoutInflater.Factory2 {

    private var viewInflater: CustomAppCompatViewInflater? = null

    private val activity: Activity?
        get() = ThemeSwitcher.findActivity(context)

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    fun showWithSkin() {
        show()
        buildContentView()
    }

    private fun buildContentView() {
        val inflater = LayoutInflater.from(context)
        val dialogInflater = inflater.cloneInContext(context)
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        val root = dialogInflater.inflate(R.layout.dialog_theme_switcher, null)
        setContentView(root)

        // 立即按当前皮肤刷一遍 + 注册独立窗口，切肤时自动跟随
        SkinManager.instance?.applySkin(root)
        SkinManager.instance?.registerWindow(root)
        updateStatus(root)

        // 弹框内也注入悬浮切肤入口（本弹框是独立 Window，Activity 的悬浮按钮被遮挡）
        (window?.decorView as? FrameLayout)?.let { ThemeSwitcher.installFabInto(it, context) }

        root.findViewById<View>(R.id.btn_theme_default)?.setOnClickListener {
            applySkin(null, R.color.colorPrimary, "default", root)
        }
        root.findViewById<View>(R.id.btn_theme_dynamic)?.setOnClickListener {
            val skinPath = "${context.applicationContext.getExternalFilesDir("skindemo")?.absolutePath}/skindemo.skin"
            applySkin(skinPath, R.color.skin_item_color, "skindemo", root)
        }
    }

    /**
     * 统一切肤入口：宿主为 [SkinActivity] 走其完整换肤（含 StatusBar/Navigation/ActionBar +
     * applyViews 遍历）；普通 Activity 直接 loadSkin，靠 SkinManager.notifySkinChange 触发监听器。
     * 比亚迪页继承 SkinActivity，走的是前者（完整换肤链路）。
     */
    private fun applySkin(skinPath: String?, themeColorId: Int, prefValue: String, root: View) {
        when (val act = activity) {
            is SkinActivity -> {
                if (skinPath == null) act.defaultSkin(themeColorId) else act.skinDynamic(skinPath, themeColorId)
            }
            else -> {
                SkinManager.instance?.loadSkin(skinPath, themeColorId)
            }
        }
        PreferencesUtils.putString(context, "currentSkin", prefValue)
        // 弹框自身内容（Skinnable* 控件）按新皮肤重刷
        SkinManager.instance?.applySkin(root)
        updateStatus(root)
    }

    private fun updateStatus(root: View) {
        val isDefault = SkinManager.instance?.currentSkinPath == null
        root.findViewById<android.widget.TextView>(R.id.tv_theme_switcher_status)?.text =
            if (isDefault) "当前：默认皮肤" else "当前：动态皮肤 (skindemo.skin)"
    }

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (name == "fragment" || name == "androidx.fragment.app.FragmentContainerView") return null
        if (viewInflater == null) viewInflater = CustomAppCompatViewInflater(context)
        viewInflater!!.setName(name)
        viewInflater!!.setAttrs(attrs)
        return viewInflater!!.autoMatch()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        onCreateView(null, name, context, attrs)
}
