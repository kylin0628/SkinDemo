package com.netease.skin.library.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
 *
 * 用法：
 * 1、继承此类
 * 2、重写openChangeSkin()方法
 */
abstract class SkinActivity : AppCompatActivity() {
    private var viewInflater: CustomAppCompatViewInflater? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val layoutInflater = LayoutInflater.from(this)
        LayoutInflaterCompat.setFactory2(layoutInflater, this)
        super.onCreate(savedInstanceState)
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
            return viewInflater!!.autoMatch()
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected fun defaultSkin(themeColorId: Int) {
        skinDynamic(null, themeColorId)
    }

    /**
     * 动态换肤（api限制：5.0版本）
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected fun skinDynamic(skinPath: String?, themeColorId: Int) {
        SkinManager.instance?.loaderSkinResources(skinPath)
        if (themeColorId != 0) {
            val themeColor: Int = SkinManager.instance!!.getColor(themeColorId)
            StatusBarUtils.forStatusBar(this, themeColor)
            NavigationUtils.forNavigation(this, themeColor)
            ActionBarUtils.forActionBar(this, themeColor)
        }
        applyViews(window.decorView)
    }

    /**
     * 控件回调监听，匹配上则给控件执行换肤方法
     */
    protected fun applyViews(view: View?) {
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