package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean: AttrsBean = AttrsBean()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun skinnableView() {
        val key =
            R.styleable.SkinnableConstraintLayout[R.styleable.SkinnableConstraintLayout_android_background]
        val backgroundResourceId = attrsBean.getViewResource(key)
        if (backgroundResourceId > 0) {
            if (SkinManager.instance!!.isDefaultSkin) {
                val drawable = ContextCompat.getDrawable(context, backgroundResourceId)
                background = drawable
            } else {
                val skinResourceId: Any =
                    SkinManager.instance!!.getBackgroundOrSrc(backgroundResourceId)!!
                if (skinResourceId is Int) {
                    setBackgroundColor(skinResourceId)
                } else {
                    val drawable = skinResourceId as Drawable
                    background = drawable
                }
            }
        }
    }

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.SkinnableConstraintLayout,
            defStyleAttr, 0
        )
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableConstraintLayout)
        typedArray.recycle()
    }
}