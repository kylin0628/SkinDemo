package com.kylin.skinlibrary

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.model.SkinCache
import com.kylin.skinlibrary.utils.SkinLog
import com.netease.skin.library.core.ViewsMatch
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 皮肤管理器
 * 加载应用资源（app内置：res/xxx） or 存储资源（下载皮肤包：skindemo.skin）
 */
class SkinManager private constructor(private val application: Application) {
    private val appResources: Resources = application.resources
    private var skinResources: Resources? = null
    private var skinPackageName: String? = ""
    var isDefaultSkin = true

    /** 当前皮肤包路径（null = 默认皮肤） */
    var currentSkinPath: String? = null
        private set

    /** 当前主题色资源 ID */
    var currentThemeColorId: Int = 0
        private set

    /**
     * 皮肤版本号：每次 loadSkin 成功加载（含切默认皮肤）后自增。
     * Compose 侧用它在 `skinnedColor()` 里作为重组触发信号（remember(key = skinVersion)）。
     */
    @Volatile
    var skinVersion: Int = 0
        private set

    /** 换肤监听器（纯 Kotlin，无 Compose 依赖；供 Compose/原生侧订阅皮肤变化） */
    private val skinChangeListeners = CopyOnWriteArrayList<() -> Unit>()

    /** 独立窗口根视图注册表：切肤时自动对其遍历换肤（WeakReference，视图回收即失效） */
    private val registeredWindows = WeakHashMap<View, Boolean>()

    /** 每个 Skinnable 控件上次换肤时的 skinVersion，用于 onAttachedToWindow 兜底重刷去重（WeakReference 自动失效） */
    private val skinnedViewVersions = WeakHashMap<ViewsMatch, Int>()

    /** 订阅皮肤变化；返回 true 表示注册成功（同一监听器不重复注册） */
    fun addSkinChangeListener(listener: () -> Unit): Boolean {
        return skinChangeListeners.addIfAbsent(listener)
    }

    fun removeSkinChangeListener(listener: () -> Unit) {
        skinChangeListeners.remove(listener)
    }

    /**
     * 注册一个独立窗口（PopupWindow / Dialog 等）的根视图，切肤时自动遍历换肤。
     *
     * 独立窗口（PopupWindow.contentView / Dialog.setContentView 的 View）拥有自己的 Window，
     * 不在 SkinActivity.applyViews(decorView) 与 applyViewsToDialogs(DialogFragment) 覆盖范围内。
     * 业务侧只需在弹框显示后调用一次本方法，此后每次 loadSkin 都会自动对其 applySkin()。
     *
     * 用途：把「独立窗口跟随换肤」下沉到主题库，业务代码无需自行管理监听器。
     * 未注册（弱引用被回收）的视图自动失效，无需显式反注册。
     */
    fun registerWindow(rootView: View) {
        synchronized(registeredWindows) {
            registeredWindows[rootView] = true
        }
    }

    private fun notifySkinChange() {
        for (listener in skinChangeListeners) {
            try {
                listener.invoke()
            } catch (e: Exception) {
                SkinLog.e(TAG, "notifySkinChange 监听器回调异常", e)
            }
        }
        // 独立窗口（PopupWindow / Dialog）跟随换肤：切肤时遍历注册的根视图。
        // 快照后遍历，避免与 registerWindow 并发修改 WeakHashMap 抛 ConcurrentModificationException，
        // 且不在持锁状态下执行耗时遍历 applySkin。
        val roots: List<View>
        synchronized(registeredWindows) {
            roots = registeredWindows.keys.toList()
        }
        SkinLog.d(TAG, "notifySkinChange → 监听器 ${skinChangeListeners.size} 个, 已注册窗口 ${roots.size} 个")
        for (root in roots) {
            try {
                applySkin(root)
            } catch (e: Exception) {
                SkinLog.e(TAG, "registerWindow 视图换肤异常", e)
            }
        }
    }

    private val cacheSkin: MutableMap<String, SkinCache> by lazy { mutableMapOf() }

    /** 宿主资源 ID → 皮肤包资源 ID 的映射缓存；皮肤切换时失效。
     *  避免每次 getColor/getDrawable 都重复 `getResourceEntryName` + `getIdentifier` 反射查找。 */
    private val skinResourceIdCache = ConcurrentHashMap<Int, Int>()

    companion object {
        private const val TAG = "SkinManager"
        private const val ADD_ASSET_PATH = "addAssetPath"

        var instance: SkinManager? = null
            private set

        fun init(application: Application) {
            if (instance == null) {
                synchronized(SkinManager::class.java) {
                    if (instance == null) {
                        instance = SkinManager(application)
                        SkinLog.i(TAG, "init() 完成，皮肤管理器已创建")
                    }
                }
            }
        }
    }

    // ==================== 皮肤加载入口 ====================

    fun loadSkin(skinPath: String?, themeColorId: Int) {
        val isSame = currentSkinPath == skinPath
        SkinLog.i(
            TAG,
            "loadSkin() 入口: skinPath=$skinPath themeColorId=$themeColorId " +
                "旧(isDefaultSkin=$isDefaultSkin, version=$skinVersion)"
        )

        currentSkinPath = skinPath
        currentThemeColorId = themeColorId
        loaderSkinResources(skinPath)
        // 皮肤资源已（可能）切换，宿主→皮肤资源 ID 映射缓存失效
        skinResourceIdCache.clear()
        skinVersion++

        SkinLog.i(
            TAG,
            "loadSkin() 完成: 新(isDefaultSkin=$isDefaultSkin, packageName=$skinPackageName, version=$skinVersion) " +
                (if (isSame) "【相同皮肤，跳过】" else "【皮肤已切换】")
        )

        // 皮肤加载完成后通知监听者（Compose 重组 / 原生侧刷肤）
        notifySkinChange()
    }

    fun loaderSkinResources(skinPath: String?) {
        if (skinPath.isNullOrEmpty()) {
            SkinLog.d(TAG, "loaderSkinResources → skinPath 为空，回退默认皮肤")
            isDefaultSkin = true
            return
        }

        if (cacheSkin.containsKey(skinPath)) {
            isDefaultSkin = false
            cacheSkin[skinPath]?.let {
                skinResources = it.skinResources
                skinPackageName = it.skinPackageName
                SkinLog.d(TAG, "loaderSkinResources → 命中缓存: packageName=$skinPackageName")
            }
            return
        }

        try {
            SkinLog.i(TAG, "loaderSkinResources → 缓存未命中，开始反射加载皮肤包: $skinPath")
            // 反射创建 AssetManager 并挂载皮肤包 APK
            val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = assetManager.javaClass.getDeclaredMethod(ADD_ASSET_PATH, String::class.java)
            addAssetPath.isAccessible = true
            addAssetPath.invoke(assetManager, skinPath)

            skinResources = Resources(assetManager, appResources.displayMetrics, appResources.configuration)

            skinPackageName = application.packageManager
                .getPackageArchiveInfo(skinPath, PackageManager.GET_ACTIVITIES)
                ?.packageName

            isDefaultSkin = skinPackageName.isNullOrEmpty()
            if (!isDefaultSkin) {
                cacheSkin[skinPath] = SkinCache(skinResources!!, skinPackageName!!)
                SkinLog.i(TAG, "loaderSkinResources → 皮肤包加载成功: packageName=$skinPackageName, path=$skinPath")
            } else {
                SkinLog.w(TAG, "loaderSkinResources → 无法获取皮肤包包名，回退默认皮肤")
            }
        } catch (e: Exception) {
            SkinLog.e(TAG, "loaderSkinResources → 皮肤包加载异常，回退默认皮肤: $skinPath", e)
            isDefaultSkin = true
        }
    }

    // ==================== 资源获取 ====================

    /**
     * 按名称映射：将宿主资源 ID 转换为皮肤包中同名资源的 ID
     */
    private fun getSkinResourceIds(resourceId: Int): Int {
        if (isDefaultSkin) return resourceId

        return skinResourceIdCache.getOrPut(resourceId) {
            val resourceName = appResources.getResourceEntryName(resourceId)
            val resourceType = appResources.getResourceTypeName(resourceId)
            val ids = skinResources!!.getIdentifier(resourceName, resourceType, skinPackageName)
            // 切主题某颜色/图片没变的最常见根因：皮肤包缺同名资源，只能回退宿主。
            // getOrPut 对同一 resourceId 仅执行一次，天然去重，不会在 getColor 热路径刷屏。
            if (ids == 0) {
                SkinLog.w(TAG, "皮肤包缺少同名资源 → $resourceType/$resourceName (hostId=$resourceId)，回退宿主")
            }
            ids
        }
    }

    /** 判断该资源应由宿主还是皮肤包提供。
     *  仅当皮肤按名查不到(ids==0)才用宿主;去掉 ids==resourceId 误判——
     *  皮肤与宿主同名资源 ID 数值会碰撞(皮肤色板派生自宿主、前段排序一致),
     *  该启发式会把皮肤里实际存在的暗色误判为"用宿主",导致基底/卡片背景永远浅色。 */
    private fun useHost(ids: Int, resourceId: Int) = ids == 0

    fun getColor(resourceId: Int): Int {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) {
            ContextCompat.getColor(application, resourceId)
        } else {
            // Resources#getColor(int, Theme) — minSdk=23 可用，非 deprecated
            skinResources!!.getColor(ids, null)
        }
    }

    fun getColorStateList(resourceId: Int): ColorStateList {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) {
            ContextCompat.getColorStateList(application, resourceId)!!
        } else {
            skinResources!!.getColorStateList(ids, null)
        }
    }

    fun getDrawableOrMipMap(resourceId: Int): Drawable {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) {
            ContextCompat.getDrawable(application, resourceId)!!
        } else {
            skinResources!!.getDrawable(ids, null)
        }
    }

    fun getString(resourceId: Int): String {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getString(resourceId)
        else skinResources!!.getString(ids)
    }

    /** 字符串（带格式化参数）。语言跟随系统 locale：宿主回退走 appResources（原生 locale 感知）。 */
    fun getString(resourceId: Int, vararg formatArgs: Any): String {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getString(resourceId, *formatArgs)
        else skinResources!!.getString(ids, *formatArgs)
    }

    fun getText(resourceId: Int): CharSequence {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getText(resourceId)
        else skinResources!!.getText(ids)
    }

    fun getDimension(resourceId: Int): Float {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getDimension(resourceId)
        else skinResources!!.getDimension(ids)
    }

    fun getDimensionPixelSize(resourceId: Int): Int {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getDimensionPixelSize(resourceId)
        else skinResources!!.getDimensionPixelSize(ids)
    }

    fun getInteger(resourceId: Int): Int {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getInteger(resourceId)
        else skinResources!!.getInteger(ids)
    }

    fun getBoolean(resourceId: Int): Boolean {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids, resourceId)) appResources.getBoolean(resourceId)
        else skinResources!!.getBoolean(ids)
    }

    /**
     * 宿主资源 ID → 皮肤包资源 ID 的公开映射入口。
     *
     * 供 Compose 侧 [SkinnableResources.getValue]/[getXml] 等「非标准 getter」复用，
     * 避免把 getIdentifier 映射逻辑复制到业务侧。返回 0 表示皮肤包缺同名资源（应回退宿主）。
     */
    fun resolveSkinId(resourceId: Int): Int = getSkinResourceIds(resourceId)

    /**
     * 皮肤包 Resources（非默认皮肤才非空）。
     *
     * 供 Compose 侧 [SkinnableResources.getValue]/[getXml] 直接读皮肤包资源（这些方法无法用
     * 单一 getter 封装，需拿到 Resources 实例）。业务侧一般无需调用。
     */
    fun getSkinResourcesOrNull(): Resources? = skinResources

    /** color / drawable / mipmap 统一获取入口 */
    fun getBackgroundOrSrc(resourceId: Int): Any? {
        return when (appResources.getResourceTypeName(resourceId)) {
            "color" -> getColor(resourceId)
            "mipmap", "drawable" -> getDrawableOrMipMap(resourceId)
            else -> null
        }
    }

    /**
     * 对任意 View 树执行换肤遍历，供独立 Window（PopupWindow / Dialog）等在切肤时刷新。
     * 与 SkinActivity.applyViews(view) 等价，但下沉到 SkinManager，便于非 Activity 组件直接调用。
     */
    fun applySkin(view: View?) {
        if (view == null) return
        if (view is ViewsMatch) {
            view.skinnableView()
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applySkin(view.getChildAt(i))
            }
        }
    }

    /**
     * 按当前皮肤刷单个控件，带 skinVersion 去重。
     *
     * 用于 Skinnable*.onAttachedToWindow() 的兜底重刷：RecyclerView 缓存/离屏复用 item 切肤时
     * 不在 applyViews 遍历范围内，attach 时需重刷。但同一个控件在一次皮肤版本内可能被多次
     * attach（如滚动复用），用本方法可跳过「皮肤未变」的重复刷，降低滚动/布局阶段的无效 invalidate。
     *
     * 首次 attach（版本未记录）或皮肤版本变化时才真正执行 skinnableView()。
     */
    fun applySkinIfChanged(view: ViewsMatch) {
        val version = skinVersion
        val last = skinnedViewVersions[view]
        if (last == version) return
        skinnedViewVersions[view] = version
        view.skinnableView()
    }

    fun getTypeface(resourceId: Int): Typeface {
        val skinTypefacePath = getString(resourceId)
        if (skinTypefacePath.isNullOrEmpty()) return Typeface.DEFAULT
        return if (isDefaultSkin) {
            Typeface.createFromAsset(appResources.assets, skinTypefacePath)
        } else {
            Typeface.createFromAsset(skinResources!!.assets, skinTypefacePath)
        }
    }
}
