package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

open class SkinnableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.imageButtonStyle
) : AppCompatImageButton(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    /**
     * 换肤适配：业务代码 setBackgroundResource 时按当前皮肤映射同名 drawable，
     * 使暗色皮肤包里颜色随深浅变化的资源生效；默认皮肤回退原生实现。
     */
    override fun setBackgroundResource(resId: Int) {
        attrsBean.updateViewResource(R.styleable.SkinnableImageButton[R.styleable.SkinnableImageButton_android_background], resId)
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
        var key = R.styleable.SkinnableImageButton[R.styleable.SkinnableImageButton_android_background]
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

        // src
        key = R.styleable.SkinnableImageButton[R.styleable.SkinnableImageButton_android_src]
        val srcResourceId = attrsBean.getViewResource(key)
        if (srcResourceId > 0) {
            if (manager.isDefaultSkin) {
                val drawable = ContextCompat.getDrawable(context, srcResourceId)
                setImageDrawable(drawable)
            } else {
                val skinResource = manager.getBackgroundOrSrc(srcResourceId)
                when (skinResource) {
                    is Int -> setImageResource(skinResource)
                    is Drawable -> setImageDrawable(skinResource)
                }
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
        context.withStyledAttributes(attrs, R.styleable.SkinnableImageButton, defStyleAttr, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableImageButton)
        }
    }
}
