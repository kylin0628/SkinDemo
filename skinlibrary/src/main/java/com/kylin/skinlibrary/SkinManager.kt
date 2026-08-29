package com.kylin.skinlibrary

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.model.SkinCache

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

        Log.d(TAG, "  新 currentSkinPath = $currentSkinPath")
        Log.d(TAG, "  新 isDefaultSkin   = $isDefaultSkin")
        Log.d(TAG, "loadSkin() 完成 ${if (isSame) "(相同皮肤，跳过)" else "(皮肤已切换!)"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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
