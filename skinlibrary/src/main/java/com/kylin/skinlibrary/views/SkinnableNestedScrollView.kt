package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        val key = R.styleable.SkinnableNestedScrollView[R.styleable.SkinnableNestedScrollView_android_background]
        val resourceId = attrsBean.getViewResource(key)
        if (resourceId > 0) {
            if (manager.isDefaultSkin) {
                val drawable = ContextCompat.getDrawable(context, resourceId)
                background = drawable
            } else {
                val skinResource = manager.getBackgroundOrSrc(resourceId)
                when (skinResource) {
                    is Int -> setBackgroundColor(skinResource)
                    is Drawable -> background = skinResource
                }
            }
        }
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableNestedScrollView, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableNestedScrollView)
        typedArray.recycle()
    }
}
