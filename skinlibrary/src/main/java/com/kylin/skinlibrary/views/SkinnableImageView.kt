package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        val key = R.styleable.SkinnableImageView[R.styleable.SkinnableImageView_android_src]
        val resourceId = attrsBean.getViewResource(key)
        if (resourceId > 0) {
            if (manager.isDefaultSkin) {
                val drawable = ContextCompat.getDrawable(context, resourceId)
                setImageDrawable(drawable)
            } else {
                val skinResource = manager.getBackgroundOrSrc(resourceId)
                when (skinResource) {
                    is Int -> setImageResource(skinResource)
                    is Drawable -> setImageDrawable(skinResource)
                }
            }
        }
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableImageView, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableImageView)
        typedArray.recycle()
    }
}
