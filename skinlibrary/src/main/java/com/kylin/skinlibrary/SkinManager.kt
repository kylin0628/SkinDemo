package com.kylin.skinlibrary

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.model.SkinCache
import com.netease.skin.library.core.ViewsMatch
import java.util.WeakHashMap
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
                Log.e(TAG, "notifySkinChange 监听器异常", e)
            }
        }
        // 独立窗口（PopupWindow / Dialog）跟随换肤：切肤时遍历注册的根视图。
        // 快照后遍历，避免与 registerWindow 并发修改 WeakHashMap 抛 ConcurrentModificationException，
        // 且不在持锁状态下执行耗时遍历 applySkin。
        val roots: List<View>
        synchronized(registeredWindows) {
            roots = registeredWindows.keys.toList()
        }
        for (root in roots) {
            try {
                applySkin(root)
            } catch (e: Exception) {
                Log.e(TAG, "registerWindow 视图换肤异常", e)
            }
        }
    }

    private val cacheSkin: MutableMap<String, SkinCache> by lazy { mutableMapOf() }

    companion object {
        private const val TAG = "[Skin] SkinManager"
        private const val ADD_ASSET_PATH = "addAssetPath"

        var instance: SkinManager? = null
            private set

        fun init(application: Application) {
            if (instance == null) {
                synchronized(SkinManager::class.java) {
                    if (instance == null) {
                        instance = SkinManager(application)
                    }
                }
            }
        }
    }

    // ==================== 皮肤加载入口 ====================

    fun loadSkin(skinPath: String?, themeColorId: Int) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "loadSkin() 入口")
        Log.d(TAG, "  skinPath     = $skinPath")
        Log.d(TAG, "  themeColorId = $themeColorId (0x${Integer.toHexString(themeColorId)})")
        Log.d(TAG, "  旧 currentSkinPath = $currentSkinPath")
        Log.d(TAG, "  旧 isDefaultSkin   = $isDefaultSkin")

        val isSame = currentSkinPath == skinPath
        currentSkinPath = skinPath
        currentThemeColorId = themeColorId
        loaderSkinResources(skinPath)
        skinVersion++

        Log.d(TAG, "  新 currentSkinPath = $currentSkinPath")
        Log.d(TAG, "  新 isDefaultSkin   = $isDefaultSkin")
        Log.d(TAG, "  skinVersion        = $skinVersion")
        Log.d(TAG, "loadSkin() 完成 ${if (isSame) "(相同皮肤，跳过)" else "(皮肤已切换!)"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // 皮肤加载完成后通知监听者（Compose 重组 / 原生侧刷肤）
        notifySkinChange()
    }

    fun loaderSkinResources(skinPath: String?) {
        Log.d(TAG, "  loaderSkinResources('$skinPath') 开始")

        if (skinPath.isNullOrEmpty()) {
            Log.d(TAG, "  loaderSkinResources → skinPath 为空，使用默认皮肤")
            isDefaultSkin = true
            return
        }

        if (cacheSkin.containsKey(skinPath)) {
            Log.d(TAG, "  loaderSkinResources → 命中缓存: $skinPath")
            isDefaultSkin = false
            cacheSkin[skinPath]?.let {
                skinResources = it.skinResources
                skinPackageName = it.skinPackageName
                Log.d(TAG, "  loaderSkinResources → 缓存加载完成, packageName=$skinPackageName")
            }
            return
        }

        try {
            Log.d(TAG, "  loaderSkinResources → 缓存未命中，开始反射加载皮肤包...")
            // 反射创建 AssetManager 并挂载皮肤包 APK
            val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = assetManager.javaClass.getDeclaredMethod(ADD_ASSET_PATH, String::class.java)
            addAssetPath.isAccessible = true
            addAssetPath.invoke(assetManager, skinPath)
            Log.d(TAG, "  loaderSkinResources → 反射 addAssetPath 成功")

            skinResources = Resources(assetManager, appResources.displayMetrics, appResources.configuration)
            Log.d(TAG, "  loaderSkinResources → Resources 创建成功")

            skinPackageName = application.packageManager
                .getPackageArchiveInfo(skinPath, PackageManager.GET_ACTIVITIES)
                ?.packageName

            Log.d(TAG, "  loaderSkinResources → 皮肤包 packageName=$skinPackageName")

            isDefaultSkin = skinPackageName.isNullOrEmpty()
            if (!isDefaultSkin) {
                cacheSkin[skinPath] = SkinCache(skinResources!!, skinPackageName!!)
                Log.d(TAG, "  loaderSkinResources → 已缓存皮肤: $skinPath")
            } else {
                Log.w(TAG, "  loaderSkinResources → 无法获取皮肤包包名，回退默认皮肤")
            }
        } catch (e: Exception) {
            Log.e(TAG, "  loaderSkinResources → 加载异常!", e)
            isDefaultSkin = true
        }
    }

    // ==================== 资源获取 ====================

    /**
     * 按名称映射：将宿主资源 ID 转换为皮肤包中同名资源的 ID
     */
    private fun getSkinResourceIds(resourceId: Int): Int {
        if (isDefaultSkin) return resourceId

        val resourceName = appResources.getResourceEntryName(resourceId)
        val resourceType = appResources.getResourceTypeName(resourceId)
        val ids = skinResources!!.getIdentifier(resourceName, resourceType, skinPackageName)
        // [DIAG] 临时诊断:名映射 + 真实 useHost 决策(修复后应 ids≠0→useHost=false→用皮肤暗)
        Log.d(TAG, "[DIAG] $resourceType/$resourceName skinId=$ids hostId=$resourceId useHost=${useHost(ids, resourceId)}")
        return ids
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
