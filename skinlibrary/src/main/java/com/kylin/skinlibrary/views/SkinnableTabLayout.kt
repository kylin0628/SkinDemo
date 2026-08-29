package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

/**
 * 换肤版 TabLayout。
 * 首页 tab_layout_main 的 tabTextColor/tabSelectedTextColor 随深浅色变化（text_secondary/text_primary），
 * 但 TabLayout 的文字颜色在 inflate 后由 material 内部固化，需在换肤时显式重设。
 */
open class SkinnableTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    /**
     * 换肤适配：业务代码 setBackgroundResource 时按当前皮肤映射同名 drawable，
     * 使暗色皮肤包里颜色随深浅变化的资源生效；默认皮肤回退原生实现。
     */
    override fun setBackgroundResource(resId: Int) {
        attrsBean.updateViewResource(R.styleable.SkinnableTabLayout[R.styleable.SkinnableTabLayout_android_background], resId)
        val manager = SkinManager.instance
        if (manager != null && !manager.isDefaultSkin) {
            when (val skinResource = manager.getBackgroundOrSrc(resId)) {
                is Int -> setBackgroundColor(skinResource)
                is Drawable -> background = skinResource
                else -> super.setBackgroundResource(resId)
            }
        } else {
            super.setBackgroundResource(resId)
        }
    }

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        // background
        val bgKey = R.styleable.SkinnableTabLayout[R.styleable.SkinnableTabLayout_android_background]
        val bgResourceId = attrsBean.getViewResource(bgKey)
        if (bgResourceId > 0) {
            if (manager.isDefaultSkin) {
                background = ContextCompat.getDrawable(context, bgResourceId)
            } else {
                when (val skinResource = manager.getBackgroundOrSrc(bgResourceId)) {
                    is Int -> setBackgroundColor(skinResource)
                    is Drawable -> background = skinResource
                }
            }
        }

        // tab 文字颜色（未选中 + 选中），需两者都重设，否则 material 会用旧值覆盖
        val normalResourceId = attrsBean.getViewResource(
            R.styleable.SkinnableTabLayout[R.styleable.SkinnableTabLayout_tabTextColor]
        )
        val selectedResourceId = attrsBean.getViewResource(
            R.styleable.SkinnableTabLayout[R.styleable.SkinnableTabLayout_tabSelectedTextColor]
        )
        val normalColor = if (normalResourceId > 0) resolve(manager, normalResourceId) else null
        val selectedColor = if (selectedResourceId > 0) resolve(manager, selectedResourceId) else null
        if (normalColor != null || selectedColor != null) {
            tabTextColors = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                intArrayOf(selectedColor ?: normalColor!!, normalColor ?: selectedColor!!)
            )
        }
    }

    private fun resolve(manager: SkinManager, resourceId: Int): Int =
        if (manager.isDefaultSkin) {
            ContextCompat.getColor(context, resourceId)
        } else {
            manager.getColor(resourceId)
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 延迟换肤兜底:RecyclerView 缓存/离屏复用持有的 holder,
        // 切主题时不在 applyViews 遍历范围内,attach 时按当前皮肤重刷一次
        skinnableView()
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableTabLayout, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableTabLayout)
        typedArray.recycle()
    }
}
