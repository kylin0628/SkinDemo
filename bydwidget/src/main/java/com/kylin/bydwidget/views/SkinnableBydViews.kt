package com.kylin.bydwidget.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.byd.widget.BydButton
import com.byd.widget.BydCardView
import com.byd.widget.BydEditText
import com.byd.widget.BydProgressBar
import com.byd.widget.BydSeekBar
import com.byd.widget.BydSwitch
import com.byd.widget.BydTextView
import com.kylin.bydwidget.R
import com.kylin.skinlibrary.SkinManager
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
 * 换肤只改文字色 / 卡片底色 / tint（比亚迪继承的公开 setter），**不碰 setBackground**，
 * 因此比亚迪的按压/缩放/白蒙层动画与过滚动回弹不受影响。
 *
 * 语义色（skin_bridge_*）定义在 bydwidget/colors.xml；皮肤包提供同名色按名映射，
 * 缺名时回退宿主（本模块）默认值。
 */
private const val BUTTON_TEXT_COLOR = Color.WHITE

/** 按当前皮肤取语义色：非默认皮肤经 SkinManager 按名映射，默认回退宿主默认值。 */
private fun skinColor(context: Context, resId: Int): Int {
    val manager = SkinManager.instance
    return if (manager != null && !manager.isDefaultSkin) {
        manager.getColor(resId)
    } else {
        ContextCompat.getColor(context, resId)
    }
}

private fun csl(color: Int): ColorStateList = ColorStateList.valueOf(color)

class SkinnableBydTextView(context: Context, attrs: AttributeSet?) :
    BydTextView(context, attrs), ViewsMatch {
    override fun skinnableView() {
        setTextColor(skinColor(context, R.color.skin_bridge_text_primary))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}

class SkinnableBydButton(context: Context, attrs: AttributeSet?) :
    BydButton(context, attrs), ViewsMatch {
    override fun skinnableView() {
        // 比亚迪按钮底色为 token 主题色，白字对比度最优
        setTextColor(BUTTON_TEXT_COLOR)
        backgroundTintList = csl(skinColor(context, R.color.skin_bridge_primary))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}

class SkinnableBydEditText(context: Context, attrs: AttributeSet?) :
    BydEditText(context, attrs), ViewsMatch {
    override fun skinnableView() {
        setTextColor(skinColor(context, R.color.skin_bridge_text_primary))
        setHintTextColor(skinColor(context, R.color.skin_bridge_text_secondary))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}

class SkinnableBydCardView(context: Context, attrs: AttributeSet?) :
    BydCardView(context, attrs), ViewsMatch {
    override fun skinnableView() {
        setCardBackgroundColor(skinColor(context, R.color.skin_bridge_card_bg))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}

class SkinnableBydProgressBar(context: Context, attrs: AttributeSet?) :
    BydProgressBar(context, attrs), ViewsMatch {
    override fun skinnableView() {
        progressTintList = csl(skinColor(context, R.color.skin_bridge_primary))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}

class SkinnableBydSeekBar(context: Context, attrs: AttributeSet?) :
    BydSeekBar(context, attrs), ViewsMatch {
    override fun skinnableView() {
        progressTintList = csl(skinColor(context, R.color.skin_bridge_primary))
        thumbTintList = csl(skinColor(context, R.color.skin_bridge_primary))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}

class SkinnableBydSwitch(context: Context, attrs: AttributeSet?) :
    BydSwitch(context, attrs), ViewsMatch {
    override fun skinnableView() {
        thumbTintList = csl(skinColor(context, R.color.skin_bridge_primary))
        trackTintList = csl(skinColor(context, R.color.skin_bridge_primary))
        setTextColor(skinColor(context, R.color.skin_bridge_text_primary))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        SkinManager.instance?.applySkinIfChanged(this)
    }
}
