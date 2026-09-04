package com.kylin.bydwidget.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.byd.widget.BydButton
import com.byd.widget.BydCardView
import com.byd.widget.BydEditText
import com.byd.widget.BydProgressBar
import com.byd.widget.BydSeekBar
import com.byd.widget.BydSwitch
import com.byd.widget.BydTextView
import com.kylin.bydwidget.R
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.model.AttrsBean
import com.netease.skin.library.core.ViewsMatch

/**
 * 比亚迪官方控件 → 主题库标准换肤链路适配。
 *
 * 背景：比亚迪控件（com.byd.widget.*，AAR 预编译）取色走 widget-tluc 内置 token，
 * 不经过 [SkinManager]，其 onConfigurationChanged 也只监听 uiMode（暗黑模式），
 * 对「动态皮肤包 / 默认主题」切换无感知。
 *
 * 为使比亚迪控件**走主题库**，这里继承比亚迪控件并实现 [ViewsMatch]：由
 * [SkinActivity] 的 Factory 责任链把 XML 标签替换成本类，切肤时 [SkinActivity.applyViews]
 * 统一遍历调用 [skinnableView] 换肤——与原生 Skinnable* 控件同一条链路，不再单独桥接。
 *
 * ## 颜色契约下沉到 XML
 *
 * 本类**不再硬编码颜色值**：换肤语义色（skin_bridge_*）由业务在 XML 布局里通过
 * 标准属性声明（android:textColor / android:progressTint / app:cardBackgroundColor 等），
 * 构造时用 [AttrsBean] 记录这些属性的资源 ID，[skinnableView] 遍历时经
 * [SkinManager.getColorStateList]/[SkinManager.getColor] 按名映射到皮肤包同名颜色。
 * 这样业务新增一个「文字用主色、提示用次级色」的控件，只需改 XML 属性，无需改本类。
 *
 * 换肤只改文字色 / 卡片底色 / tint（比亚迪继承的公开 setter），**不碰 setBackground**，
 * 因此比亚迪的按压/缩放/白蒙层动画与过滚动回弹不受影响。
 */
class SkinnableBydTextView(context: Context, attrs: AttributeSet?) :
    BydTextView(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val resId = attrsBean.getViewResource(
            R.styleable.SkinnableBydTextView[R.styleable.SkinnableBydTextView_android_textColor]
        )
        if (resId > 0) setTextColor(manager.getColorStateList(resId))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydTextView, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydTextView)
        }
    }
}

class SkinnableBydButton(context: Context, attrs: AttributeSet?) :
    BydButton(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val textColorResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydButton[R.styleable.SkinnableBydButton_android_textColor]
        )
        if (textColorResId > 0) setTextColor(manager.getColorStateList(textColorResId))
        val tintResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydButton[R.styleable.SkinnableBydButton_android_backgroundTint]
        )
        if (tintResId > 0) backgroundTintList = manager.getColorStateList(tintResId)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydButton, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydButton)
        }
    }
}

class SkinnableBydEditText(context: Context, attrs: AttributeSet?) :
    BydEditText(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val textColorResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydEditText[R.styleable.SkinnableBydEditText_android_textColor]
        )
        if (textColorResId > 0) setTextColor(manager.getColorStateList(textColorResId))
        val hintResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydEditText[R.styleable.SkinnableBydEditText_android_textColorHint]
        )
        if (hintResId > 0) setHintTextColor(manager.getColorStateList(hintResId))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydEditText, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydEditText)
        }
    }
}

class SkinnableBydCardView(context: Context, attrs: AttributeSet?) :
    BydCardView(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val resId = attrsBean.getViewResource(
            R.styleable.SkinnableBydCardView[R.styleable.SkinnableBydCardView_cardBackgroundColor]
        )
        if (resId > 0) setCardBackgroundColor(manager.getColor(resId))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydCardView, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydCardView)
        }
    }
}

class SkinnableBydProgressBar(context: Context, attrs: AttributeSet?) :
    BydProgressBar(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val resId = attrsBean.getViewResource(
            R.styleable.SkinnableBydProgressBar[R.styleable.SkinnableBydProgressBar_android_progressTint]
        )
        if (resId > 0) progressTintList = manager.getColorStateList(resId)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydProgressBar, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydProgressBar)
        }
    }
}

class SkinnableBydSeekBar(context: Context, attrs: AttributeSet?) :
    BydSeekBar(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val progressResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydSeekBar[R.styleable.SkinnableBydSeekBar_android_progressTint]
        )
        if (progressResId > 0) progressTintList = manager.getColorStateList(progressResId)
        val thumbResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydSeekBar[R.styleable.SkinnableBydSeekBar_android_thumbTint]
        )
        if (thumbResId > 0) thumbTintList = manager.getColorStateList(thumbResId)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydSeekBar, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydSeekBar)
        }
    }
}

class SkinnableBydSwitch(context: Context, attrs: AttributeSet?) :
    BydSwitch(context, attrs), ViewsMatch {
    private val attrsBean = AttrsBean()

    override fun skinnableView() {
        val manager = SkinManager.instance ?: return
        val thumbResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydSwitch[R.styleable.SkinnableBydSwitch_android_thumbTint]
        )
        if (thumbResId > 0) thumbTintList = manager.getColorStateList(thumbResId)
        val trackResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydSwitch[R.styleable.SkinnableBydSwitch_android_trackTint]
        )
        if (trackResId > 0) trackTintList = manager.getColorStateList(trackResId)
        val textColorResId = attrsBean.getViewResource(
            R.styleable.SkinnableBydSwitch[R.styleable.SkinnableBydSwitch_android_textColor]
        )
        if (textColorResId > 0) setTextColor(manager.getColorStateList(textColorResId))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkinnableBydSwitch, 0, 0) {
            attrsBean.saveViewResource(this, R.styleable.SkinnableBydSwitch)
        }
    }
}
