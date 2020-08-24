package com.kylin.skinlibrary.core

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatViewInflater
import com.kylin.skinlibrary.utils.SystemViewName
import com.kylin.skinlibrary.views.*

/**
 * 自定义控件加载器（可以考虑该类不被继承）
 */
class CustomAppCompatViewInflater(  // 上下文
    private val context: Context
) : AppCompatViewInflater() {
    private var name // 控件名
            : String? = null
    private var attrs // 某控件对应所有属性
            : AttributeSet? = null

    fun setName(name: String?) {
        this.name = name
    }

    fun setAttrs(attrs: AttributeSet?) {
        this.attrs = attrs
    }

    /**
     * @return 自动匹配控件名，并初始化控件对象
     */
    fun autoMatch(): View? {
        var view: View? = null
        when (name) {
            SystemViewName.NESTED_SCROLL_VIEW -> {
                // view = super.createTextView(context, attrs); // 源码写法
                view = SkinnableNestedScrollView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.CONSTRAINT_LAYOUT -> {
                // view = super.createTextView(context, attrs); // 源码写法
                view = SkinnableConstraintLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.LINEAR_LAYOUT -> {
                // view = super.createTextView(context, attrs); // 源码写法
                view = SkinnableLinearLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.RELATIVE_LAYOUT -> {
                view = SkinnableRelativeLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TEXT_VIEW -> {
                view = SkinnableTextView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.IMAGE_VIEW -> {
                view = SkinnableImageView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.MATERIAL_BUTTON, SystemViewName.BUTTON -> {
                view = SkinnableButton(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.APPCOMPAT_EDIT_TEXT -> {
                view = SkinnableEditText(context, attrs)
                verifyNotNull(view, name)
            }
        }
        return view
    }

    /**
     * 校验控件不为空（源码方法，由于private修饰，只能复制过来了。为了代码健壮，可有可无）
     *
     * @param view 被校验控件，如：AppCompatTextView extends TextView（v7兼容包，兼容是重点！！！）
     * @param name 控件名，如："ImageView"
     */
    private fun verifyNotNull(view: View?, name: String?) {
        checkNotNull(view) { this.javaClass.name + " asked to inflate view for <" + name + ">, but returned null" }
    }
}