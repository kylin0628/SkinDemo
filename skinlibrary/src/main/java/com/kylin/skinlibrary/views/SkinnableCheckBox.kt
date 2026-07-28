package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.checkboxStyle
) : AppCompatCheckBox(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        // background
        var key = R.styleable.SkinnableCheckBox[R.styleable.SkinnableCheckBox_android_background]
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
        key = R.styleable.SkinnableCheckBox[R.styleable.SkinnableCheckBox_android_textColor]
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
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableCheckBox, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableCheckBox)
        typedArray.recycle()
    }
}
