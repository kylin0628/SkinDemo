package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
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
     * 运行时按资源 ID 设色（显式传 @ColorRes）。
     *
     * 与系统 `setTextColor(context.getColor(id))` 的区别：本方法显式携带资源 ID，回填 attrsBean 后
     * 切肤遍历 skinnableView() 能精确按名重映射，不依赖「从 ARGB 反推资源」，从根本上消除
     * 「两个不同资源解析出同一 ARGB 时无法区分」导致的串色（见 SkinColorCrossTalkDemo）。
     *
     * 业务在「运行时设色且希望该文字跟随换肤」处，用本方法替换 `setTextColor(getColor(...))`；
     * 其余纯字面量/一次性设色仍用系统 setTextColor。
     */
    fun setTextColorRes(resId: Int) {
        val manager = SkinManager.instance ?: return
        attrsBean.updateViewResource(
            R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_textColor],
            resId
        )
        setTextColor(manager.getColorStateList(resId))
    }

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        // background
        var key = R.styleable.SkinnableTextView[R.styleable.SkinnableTextView_android_background]
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
        context.withStyledAttributes(attrs, R.styleable.SkinnableTextView, defStyleAttr, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableTextView)
        }
    }
}
