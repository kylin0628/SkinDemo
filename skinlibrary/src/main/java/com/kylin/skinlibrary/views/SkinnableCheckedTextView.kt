package com.kylin.skinlibrary.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.CheckedTextView
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableCheckedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.checkedTextViewStyle
) : CheckedTextView(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return

        // background
        var key = R.styleable.SkinnableCheckedTextView[R.styleable.SkinnableCheckedTextView_android_background]
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
        key = R.styleable.SkinnableCheckedTextView[R.styleable.SkinnableCheckedTextView_android_textColor]
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
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableCheckedTextView, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableCheckedTextView)
        typedArray.recycle()
    }
}
