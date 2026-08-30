package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.model.AttrsBean
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch

open class SkinnableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    /**
     * 换肤适配：业务代码 setBackgroundResource 时按当前皮肤映射同名 drawable，
     * 使暗色皮肤包里颜色随深浅变化的资源生效；默认皮肤回退原生实现。
     */
    override fun setBackgroundResource(resId: Int) {
        attrsBean.updateViewResource(R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_background], resId)
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

    /**
     * 按资源 ID 设置文字颜色并纳入换肤管理。
     *
     * 背景：业务代码 `setTextColor(int)` 接收的是已解析的 ARGB 值（资源 ID 在 getColor() 那一刻已丢失），
     * 无法像 setBackgroundResource 那样靠 override 反推映射。改用本方法传资源 ID，
     * 记录到 attrsBean 供 skinnableView() 遍历重刷，并即时按当前皮肤映射颜色。
     *
     * 用法：`skinnableTextView.setTextColorRes(R.color.xxx)`，替代 `setTextColor(context.getColor(R.color.xxx))`。
     */
    fun setTextColorRes(@ColorRes resId: Int) {
        attrsBean.updateViewResource(
            R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_textColor],
            resId
        )
        val manager = SkinManager.instance
        if (manager != null && !manager.isDefaultSkin) {
            setTextColor(manager.getColor(resId))
        } else {
            setTextColor(ContextCompat.getColor(context, resId))
        }
    }

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        // background
        var key = R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_background]
        val backgroundResourceId = attrsBean.getViewResource(key)
        if (backgroundResourceId > 0) {
            if (manager.isDefaultSkin) {
                val drawable = ContextCompat.getDrawable(context, backgroundResourceId)
                setBackgroundDrawable(drawable)
            } else {
                val skinResource = manager.getBackgroundOrSrc(backgroundResourceId)
                when (skinResource) {
                    is Int -> setBackgroundColor(skinResource)
                    is Drawable -> setBackgroundDrawable(skinResource)
                }
            }
        }

        // textColor
        key = R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_textColor]
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

        // textColorHint
        key = R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_textColorHint]
        val textColorHintResourceId = attrsBean.getViewResource(key)
        if (textColorHintResourceId > 0) {
            if (manager.isDefaultSkin) {
                val color = ContextCompat.getColorStateList(context, textColorHintResourceId)
                setHintTextColor(color)
            } else {
                val color: ColorStateList = manager.getColorStateList(textColorHintResourceId)
                setHintTextColor(color)
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
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableTextView, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableTextView)
        typedArray.recycle()
    }
}
