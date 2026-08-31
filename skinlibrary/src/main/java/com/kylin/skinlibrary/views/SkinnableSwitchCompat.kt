package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

open class SkinnableSwitchCompat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.switchStyle
) : SwitchCompat(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    /**
     * 换肤适配：业务代码 setBackgroundResource 时按当前皮肤映射同名 drawable，
     * 使暗色皮肤包里颜色随深浅变化的资源生效；默认皮肤回退原生实现。
     */
    override fun setBackgroundResource(resId: Int) {
        attrsBean.updateViewResource(R.styleable.SkinnableSwitchCompat[R.styleable.SkinnableSwitchCompat_android_background], resId)
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
        var key = R.styleable.SkinnableSwitchCompat[R.styleable.SkinnableSwitchCompat_android_background]
        val backgroundResourceId = attrsBean.getViewResource(key)
        if (backgroundResourceId > 0) {
            if (manager.isDefaultSkin) {
                val drawable = ContextCompat.getDrawable(context, backgroundResourceId)
                setBackground(drawable!!)
            } else {
                val skinResource = manager.getBackgroundOrSrc(backgroundResourceId)
                when (skinResource) {
                    is Int -> setBackgroundColor(skinResource)
                    is Drawable -> setBackground(skinResource)
                }
            }
        }

        // textColor
        key = R.styleable.SkinnableSwitchCompat[R.styleable.SkinnableSwitchCompat_android_textColor]
        val textColorResourceId = attrsBean.getViewResource(key)
        if (textColorResourceId > 0) {
            if (manager.isDefaultSkin) {
                val color = ContextCompat.getColorStateList(context, textColorResourceId)
                setTextColor(color)
            } else {
                val color: ColorStateList = manager.getColorStateList(textColorResourceId)
                setTextColor(color)
            }
        }

        // thumbTint（开关滑块着色，随主题切换）
        key = R.styleable.SkinnableSwitchCompat[R.styleable.SkinnableSwitchCompat_android_thumbTint]
        val thumbTintResourceId = attrsBean.getViewResource(key)
        if (thumbTintResourceId > 0) {
            if (manager.isDefaultSkin) {
                setThumbTintList(ContextCompat.getColorStateList(context, thumbTintResourceId))
            } else {
                setThumbTintList(manager.getColorStateList(thumbTintResourceId))
            }
        }

        // trackTint（开关轨道着色，随主题切换）
        key = R.styleable.SkinnableSwitchCompat[R.styleable.SkinnableSwitchCompat_android_trackTint]
        val trackTintResourceId = attrsBean.getViewResource(key)
        if (trackTintResourceId > 0) {
            if (manager.isDefaultSkin) {
                setTrackTintList(ContextCompat.getColorStateList(context, trackTintResourceId))
            } else {
                setTrackTintList(manager.getColorStateList(trackTintResourceId))
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 延迟换肤兜底:RecyclerView 缓存/离屏复用持有的 holder,
        // 切主题时不在 applyViews 遍历范围内,attach 时按当前皮肤重刷一次
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableSwitchCompat, defStyleAttr, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableSwitchCompat)
        }
    }
}
