package com.kylin.skinlibrary

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
class SkinManager private constructor(
    private val application: Application,
    private val config: SkinConfig = SkinConfig(),
) {
    private val appResources: Resources = application.resources
    private var skinResources: Resources? = null
    private var skinPackageName: String? = ""
    var isDefaultSkin = true

    /** 当前皮肤包路径（null = 默认皮肤） */
    var currentSkinPath: String? = null
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

        // 应用 APK（含皮肤包）资源的 package id：AAPT 固定把 app 资源编到 0x7f（framework 0x01、
        // 厂商 overlay 更低的 id），故 0x7f 唯一对应皮肤包包名。
        private const val APP_PACKAGE_ID = 0x7f

        var instance: SkinManager? = null
            private set

        /**
         * 初始化皮肤管理器。
         *
         * @param config 资源类型开关（图片 / 颜色 / 字符串 / 尺寸是否换肤）；不传默认四类全支持。
         *               仅首次调用生效，重复调用不覆盖已创建的实例。
         */
        fun init(application: Application, config: SkinConfig = SkinConfig()) {
            if (instance == null) {
                synchronized(SkinManager::class.java) {
                    if (instance == null) {
                        instance = SkinManager(application, config)
                        SkinLog.i(TAG, "init() 完成，皮肤管理器已创建: $config")
                    }
                }
            }
        }
    }

    // ==================== 皮肤加载入口 ====================

    /**
     * @return true=皮肤确实发生切换（已重载资源并通知监听者）；false=相同皮肤，直接跳过。
     *         调用方（如 [com.netease.skin.library.base.SkinActivity.skinDynamic]）可据此跳过无谓重刷。
     */
    fun loadSkin(skinPath: String?): Boolean {
        val isSame = currentSkinPath == skinPath
        SkinLog.i(
            TAG,
            "loadSkin() 入口: skinPath=$skinPath " +
                "旧(isDefaultSkin=$isDefaultSkin, version=$skinVersion)"
        )

        // 相同皮肤直接短路：不再重载资源、不再 skinVersion++、不再遍历已注册窗口。
        // 修复「onResume 兜底每次切回都全量重刷」的根因（此前 isSame 仅用于日志，实际仍会重刷）。
        if (isSame) {
            SkinLog.i(TAG, "loadSkin() → 相同皮肤，跳过（不重载、不通知、不重刷）")
            return false
        }

        loaderSkinResources(skinPath)
        // 加载结果同步路径：成功加载非默认皮肤才记录路径；回退默认/加载失败均视为默认（路径置空）。
        // 修复「currentSkinPath 先写路径、加载失败不回滚」导致 isDefaultSkin=true 与
        // currentSkinPath!=null 矛盾，使 updateStatus 显示「动态皮肤」而实际走宿主资源。
        currentSkinPath = if (isDefaultSkin) null else skinPath

        // 皮肤资源已切换，宿主→皮肤资源 ID 映射缓存失效
        skinResourceIdCache.clear()
        skinVersion++

        SkinLog.i(
            TAG,
            "loadSkin() 完成: 新(isDefaultSkin=$isDefaultSkin, packageName=$skinPackageName, version=$skinVersion)"
        )

        // 皮肤加载完成后通知监听者（Compose 重组 / 原生侧刷肤）
        notifySkinChange()
        return true
    }

    fun loaderSkinResources(skinPath: String?) {
        if (skinPath.isNullOrEmpty()) {
            SkinLog.d(TAG, "loaderSkinResources → skinPath 为空，回退默认皮肤")
            resetSkinResources()
            return
        }

        if (cacheSkin.containsKey(skinPath)) {
            cacheSkin[skinPath]?.let {
                skinResources = it.skinResources
                skinPackageName = it.skinPackageName
                isDefaultSkin = false
                SkinLog.d(TAG, "loaderSkinResources → 命中缓存: packageName=$skinPackageName")
            } ?: run {
                // 缓存项异常为空：视为加载失败，回退默认皮肤（保持 skinResources 与 isDefaultSkin 一致）
                SkinLog.w(TAG, "loaderSkinResources → 缓存命中但值为空，回退默认皮肤")
                resetSkinResources()
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

            @Suppress("DEPRECATION")
            val resources = Resources(assetManager, appResources.displayMetrics, appResources.configuration)

            val packageName = resolveSkinPackageName(assetManager, skinPath)

            if (packageName.isNullOrEmpty()) {
                SkinLog.w(TAG, "loaderSkinResources → 无法获取皮肤包包名，回退默认皮肤")
                resetSkinResources()
            } else {
                skinResources = resources
                skinPackageName = packageName
                isDefaultSkin = false
                cacheSkin[skinPath] = SkinCache(resources, packageName)
                SkinLog.i(TAG, "loaderSkinResources → 皮肤包加载成功: packageName=$packageName, path=$skinPath")
            }
        } catch (e: Exception) {
            SkinLog.e(TAG, "loaderSkinResources → 皮肤包加载异常，回退默认皮肤: $skinPath", e)
            resetSkinResources()
        }
    }

    /** 统一回退默认皮肤：清空皮肤包 Resources/包名并置默认标志，避免状态残留（见 loadSkin 状态机说明）。 */
    private fun resetSkinResources() {
        skinResources = null
        skinPackageName = null
        isDefaultSkin = true
    }

    /**
     * 读取皮肤包 APK 的包名，用于后续 `getIdentifier(name, type, packageName)` 按名映射。
     *
     * 皮肤包是标准应用 APK，其资源固定编在 application 包 id [APP_PACKAGE_ID]（0x7f）。
     * 故优先反射 [AssetManager.getAssignedPackageIdentifiers]（返回 SparseArray：key=包 id，
     * value=包名）按 0x7f 精准取值，绕开 [android.content.pm.PackageManager.getPackageArchiveInfo]
     * ——后者在部分机型（vivo / 联发科，Android 14/15）会触发框架 `ParsingPackageUtils` 静态初始化
     * 去读 `/vendor/etc/aconfig_flags.pb`，文件缺失打印 ENOENT 错误（设备固件噪音，但污染日志）。
     *
     * 注意：必须按 0x7f 取值，不能「取第一个非 android」——AssetManager 上除 framework("android")
     * 外还挂着厂商 overlay（如 com.mediatek.frameworkresoverlay，id 更低），顺序因设备而异，
     * 取错会让皮肤整体失效。反射失败（极端定制系统）时回落 getPackageArchiveInfo 兜底。
     */
    private fun resolveSkinPackageName(assetManager: AssetManager, skinPath: String): String? {
        try {
            val method = AssetManager::class.java.getDeclaredMethod("getAssignedPackageIdentifiers")
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val identifiers = method.invoke(assetManager) as? SparseArray<*>
            val name = identifiers?.get(APP_PACKAGE_ID) as? String
            if (!name.isNullOrEmpty()) return name
        } catch (e: Exception) {
            SkinLog.e(TAG, "反射读取皮肤包包名失败，回落 getPackageArchiveInfo", e)
        }
        // 兜底：标准路径（可能触发系统 AconfigFlags 噪音日志，但不影响结果）
        return try {
            @Suppress("DEPRECATION")
            application.packageManager
                .getPackageArchiveInfo(skinPath, PackageManager.GET_ACTIVITIES)
                ?.packageName
        } catch (e: Exception) {
            SkinLog.e(TAG, "getPackageArchiveInfo 读取皮肤包包名异常", e)
            null
        }
    }

    // ==================== 资源获取 ====================

    /**
     * 按名称映射：将宿主资源 ID 转换为皮肤包中同名资源的 ID
     */
    private fun getSkinResourceIds(resourceId: Int): Int {
        if (isDefaultSkin) return resourceId

        // 只对「宿主 App 自身资源」尝试换肤。framework（android.R）与厂商（如 vivo）资源
        // 的同名资源不存在于皮肤包，硬查只会刷「皮肤包缺少同名资源」日志且必回退宿主，
        // 故直接短路，根除冷启动/切肤时的海量噪音日志（dimen/config_*、ic_ab_back_material 等）。
        if (appResources.getResourcePackageName(resourceId) != application.packageName) return 0

        val resourceType = appResources.getResourceTypeName(resourceId)
        // 初始化时若关闭了该资源类型的换肤，返回 0 等价于「皮肤包缺同名资源」，
        // 使 getter（useHost）与 resolveSkinId 调用方（SkinnableResources.getValue/getXml）统一回退宿主。
        if (!supportsResourceType(resourceType)) return 0

        return skinResourceIdCache.getOrPut(resourceId) {
            val resourceName = appResources.getResourceEntryName(resourceId)
            val ids = skinResources!!.getIdentifier(resourceName, resourceType, skinPackageName)
            // 切主题某颜色/图片没变的最常见根因：皮肤包缺同名资源，只能回退宿主。
            // getOrPut 对同一 resourceId 仅执行一次，天然去重，不会在 getColor 热路径刷屏。
            if (ids == 0) {
                SkinLog.w(TAG, "皮肤包缺少同名资源 → $resourceType/$resourceName (hostId=$resourceId)，回退宿主")
            }
            ids
        }
    }

    /** 该资源类型是否参与换肤（由初始化时的 [SkinConfig] 决定，默认四类全支持）。 */
    private fun supportsResourceType(resourceType: String): Boolean {
        return when (resourceType) {
            "drawable", "mipmap" -> config.supportDrawable
            "color" -> config.supportColor
            "string" -> config.supportString
            "dimen" -> config.supportDimen
            else -> true
        }
    }

    /** 判断该资源应由宿主还是皮肤包提供。
     *  1) 默认皮肤恒用宿主（此时 skinResources 为 null，不可访问）。
     *  2) 非默认皮肤：仅当皮肤包按名查不到(ids==0)才回退宿主；去掉 ids==resourceId 误判——
     *     皮肤与宿主同名资源 ID 数值会碰撞(皮肤色板派生自宿主、前段排序一致),
     *     该启发式会把皮肤里实际存在的暗色误判为"用宿主",导致基底/卡片背景永远浅色。 */
    private fun useHost(ids: Int) = isDefaultSkin || ids == 0

    fun getColor(resourceId: Int): Int {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) {
            ContextCompat.getColor(application, resourceId)
        } else {
            // Resources#getColor(int, Theme) — minSdk=23 可用，非 deprecated
            skinResources!!.getColor(ids, null)
        }
    }

    fun getColorStateList(resourceId: Int): ColorStateList {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) {
            ContextCompat.getColorStateList(application, resourceId)!!
        } else {
            skinResources!!.getColorStateList(ids, null)
        }
    }

    fun getDrawableOrMipMap(resourceId: Int): Drawable {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) {
            ContextCompat.getDrawable(application, resourceId)!!
        } else {
            skinResources!!.getDrawable(ids, null)
        }
    }

    fun getString(resourceId: Int): String {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) appResources.getString(resourceId)
        else skinResources!!.getString(ids)
    }

    /** 字符串（带格式化参数）。语言跟随系统 locale：宿主回退走 appResources（原生 locale 感知）。 */
    fun getString(resourceId: Int, vararg formatArgs: Any): String {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) appResources.getString(resourceId, *formatArgs)
        else skinResources!!.getString(ids, *formatArgs)
    }

    fun getText(resourceId: Int): CharSequence {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) appResources.getText(resourceId)
        else skinResources!!.getText(ids)
    }

    fun getDimension(resourceId: Int): Float {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) appResources.getDimension(resourceId)
        else skinResources!!.getDimension(ids)
    }

    fun getDimensionPixelSize(resourceId: Int): Int {
        val ids = getSkinResourceIds(resourceId)
        return if (useHost(ids)) appResources.getDimensionPixelSize(resourceId)
        else skinResources!!.getDimensionPixelSize(ids)
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
     * 统一文本换肤入口：按名映射重刷 TextView 的 text 与 textSize。
     *
     * 供 Skinnable*.skinnableView() 复用，避免 13 个文本控件重复 getString/getDimension 逻辑。
     * 仅当资源 ID > 0（即 XML 里显式写 `@string`/`@dimen` 引用）才重刷；
     * 字面量（`android:text="…"`、`android:textSize="16sp"`）经 getResourceId 返回 -1，自动跳过，
     * 从而不覆盖业务代码运行时 setText 的动态文本。
     */
    fun applyTextSkin(view: TextView, textRes: Int, textSizeRes: Int) {
        if (textRes > 0) view.text = getString(textRes)
        if (textSizeRes > 0) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimension(textSizeRes))
    }

    /** 按名映射重刷 TextView 的 hint（输入框提示文案），仅资源 ID > 0（显式 `@string` 引用）才生效。 */
    fun applyHintSkin(view: TextView, hintRes: Int) {
        if (hintRes > 0) view.hint = getString(hintRes)
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
}
