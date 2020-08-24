package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

class SkinnableRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean: AttrsBean = AttrsBean()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun skinnableView() {
        // 根据自定义属性，获取styleable中的background属性
        val key =
            R.styleable.SkinnableRelativeLayout[R.styleable.SkinnableRelativeLayout_android_background]
        // 根据styleable获取控件某属性的resourceId
        val backgroundResourceId = attrsBean.getViewResource(key)
        if (backgroundResourceId > 0) {
            // 是否默认皮肤
            if (SkinManager.instance!!.isDefaultSkin) {
                // 兼容包转换
                val drawable = ContextCompat.getDrawable(context, backgroundResourceId)
                // 控件自带api，这里不用setBackgroundColor()因为在9.0测试不通过
                // setBackgroundDrawable在这里是过时了
                background = drawable
            } else {
                // 获取皮肤包资源
                val skinResourceId: Any =
                    SkinManager.instance!!.getBackgroundOrSrc(backgroundResourceId)!!
                // 兼容包转换
                if (skinResourceId is Int) {
                    setBackgroundColor(skinResourceId)
                    // setBackgroundResource(color); // 未做兼容测试
                } else {
                    val drawable = skinResourceId as Drawable
                    background = drawable
                }
            }
        }
    }

    init {

        // 根据自定义属性，匹配控件属性的类型集合，如：background
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.SkinnableRelativeLayout,
            defStyleAttr, 0
        )
        // 存储到临时JavaBean对象
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableRelativeLayout)
        // 这一句回收非常重要！obtainStyledAttributes()有语法提示！！
        typedArray.recycle()
    }
}