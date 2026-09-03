package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import android.widget.ProgressBar
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

open class SkinnableProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ProgressBar(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    /**
     * 换肤适配：业务代码 setBackgroundResource 时按当前皮肤映射同名 drawable，
     * 使暗色皮肤包里颜色随深浅变化的资源生效；默认皮肤回退原生实现。
     */
    override fun setBackgroundResource(resId: Int) {
        attrsBean.updateViewResource(R.styleable.SkinnableProgressBar[R.styleable.SkinnableProgressBar_android_background], resId)
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
        val bgKey = R.styleable.SkinnableProgressBar[R.styleable.SkinnableProgressBar_android_background]
        val resourceId = attrsBean.getViewResource(bgKey)
        if (resourceId > 0) {
            if (manager.isDefaultSkin) {
                background = ContextCompat.getDrawable(context, resourceId)
            } else {
                when (val skinResource = manager.getBackgroundOrSrc(resourceId)) {
                    is Int -> setBackgroundColor(skinResource)
                    is Drawable -> background = skinResource
                }
            }
        }

        // 进度条四类 tint 都随主题切换（ProgressBar 本身只有进度没有文字，主要靠着色体现变化）
        applyTint(
            manager,
            R.styleable.SkinnableProgressBar[R.styleable.SkinnableProgressBar_android_progressTint]
        ) { setProgressTintList(it) }
        applyTint(
            manager,
            R.styleable.SkinnableProgressBar[R.styleable.SkinnableProgressBar_android_progressBackgroundTint]
        ) { setProgressBackgroundTintList(it) }
        applyTint(
            manager,
            R.styleable.SkinnableProgressBar[R.styleable.SkinnableProgressBar_android_secondaryProgressTint]
        ) { setSecondaryProgressTintList(it) }
        applyTint(
            manager,
            R.styleable.SkinnableProgressBar[R.styleable.SkinnableProgressBar_android_indeterminateTint]
        ) { setIndeterminateTintList(it) }
    }

    private fun applyTint(
        manager: SkinManager,
        styleableKey: Int,
        apply: (ColorStateList) -> Unit
    ) {
        val resourceId = attrsBean.getViewResource(styleableKey)
        if (resourceId <= 0) return
        apply(
            if (manager.isDefaultSkin) {
                ContextCompat.getColorStateList(context, resourceId)!!
            } else {
                manager.getColorStateList(resourceId)
            }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 延迟换肤兜底:RecyclerView 缓存/离屏复用持有的 holder,
        // 切主题时不在 applyViews 遍历范围内,attach 时按当前皮肤重刷一次
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableProgressBar, defStyleAttr, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableProgressBar)
        }
    }
}
