package com.kylin.skinlibrary.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.kylin.skinlibrary.R
import com.kylin.skinlibrary.SkinManager
import com.netease.skin.library.core.ViewsMatch
import com.kylin.skinlibrary.model.AttrsBean

/**
 * 换肤版 View。
 * 首页搜索框 view_main_search 是普通 <View>（背景 bg_search_view），框架默认不匹配普通 View，
 * 需单独匹配才能换搜索框背景色。
 */
open class SkinnableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ViewsMatch {
    private val attrsBean = AttrsBean()

    /**
     * 换肤适配：业务代码 setBackgroundResource 时按当前皮肤映射同名 drawable，
     * 使暗色皮肤包里颜色随深浅变化的资源生效；默认皮肤回退原生实现。
     */
    override fun setBackgroundResource(resId: Int) {
        attrsBean.updateViewResource(R.styleable.SkinnableView[R.styleable.SkinnableView_android_background], resId)
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

        val key = R.styleable.SkinnableView[R.styleable.SkinnableView_android_background]
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 延迟换肤兜底:RecyclerView 缓存/离屏复用持有的 holder,
        // 切主题时不在 applyViews 遍历范围内,attach 时按当前皮肤重刷一次
        skinnableView()
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SkinnableView, defStyleAttr, 0)
        attrsBean.saveViewResource(typedArray, R.styleable.SkinnableView)
        typedArray.recycle()
    }
}
