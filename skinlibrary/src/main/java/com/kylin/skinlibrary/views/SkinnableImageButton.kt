package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.imageButtonStyle
) : AppCompatImageButton(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

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

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableImageButton, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableImageButton)
        typedArray.recycle()
    }
}
