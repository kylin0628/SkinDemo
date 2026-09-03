package com.kylin.bydwidget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import com.kylin.bydwidget.views.SkinnableBydButton
import com.kylin.bydwidget.views.SkinnableBydCardView
import com.kylin.bydwidget.views.SkinnableBydEditText
import com.kylin.bydwidget.views.SkinnableBydProgressBar
import com.kylin.bydwidget.views.SkinnableBydSeekBar
import com.kylin.bydwidget.views.SkinnableBydSwitch
import com.kylin.bydwidget.views.SkinnableBydTextView
import com.kylin.skinlibrary.SkinUiHost
import com.netease.skin.library.base.SkinActivity

/**
 * 比亚迪官方控件演示页。
 *
 * **继承 [SkinActivity] 走主题库标准换肤链路**：重写 [createSkinnableView] 把 XML 标签
 * 替换成 SkinnableByd*（继承比亚迪控件 + 实现 ViewsMatch），切肤时由 SkinActivity 的
 * `applyViews(decorView)` 统一遍历调用 `skinnableView()` 换肤，与原生 Skinnable* 控件同一条链路。
 *
 * 相比旧的 BydThemeBridge（手动遍历 + 监听器桥接），本方案：
 *  - 换肤遍历、去重、弹框跟随等逻辑全部复用主题库，bywdidget 不再自维护监听器；
 *  - 控件替换也走 SkinActivity.onCreateView 的统一入口，而非另设 LayoutInflater.Factory2。
 *
 * 比亚迪控件取色走 widget-tluc 内置 token，对动态皮肤包无感知，故 SkinnableByd*.skinnableView()
 * 用 SkinManager 取语义色（skin_bridge_*）刷到公开 setter，不碰 setBackground，按压/缩放/白蒙层
 * 动画不受影响。
 */
class BydWidgetDemoActivity : SkinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_byd_widget_demo)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 全局主题切换悬浮按钮：比亚迪页同样走宿主实现切肤
        SkinUiHost.installThemeSwitcher?.invoke(this)
    }

    /**
     * 比亚迪控件替换：XML 标签名 → SkinnableByd*（实现 ViewsMatch，进入主题库统一换肤）。
     * 未命中的控件回落 super（CustomAppCompatViewInflater → Skinnable*），保证页面里
     * 其它普通控件仍走原生换肤。
     */
    override fun createSkinnableView(name: String, context: Context, attrs: AttributeSet): View? {
        return when (name) {
            "TextView", "android.widget.TextView" -> SkinnableBydTextView(context, attrs)

            "Button",
            "android.widget.Button",
            "androidx.appcompat.widget.AppCompatButton" -> SkinnableBydButton(context, attrs)

            "EditText",
            "android.widget.EditText",
            "com.google.android.material.textfield.TextInputEditText" -> SkinnableBydEditText(context, attrs)

            "ProgressBar", "android.widget.ProgressBar" -> SkinnableBydProgressBar(context, attrs)

            "SeekBar",
            "android.widget.SeekBar",
            "androidx.appcompat.widget.AppCompatSeekBar" -> SkinnableBydSeekBar(context, attrs)

            "Switch", "android.widget.Switch" -> SkinnableBydSwitch(context, attrs)

            "com.google.android.material.card.MaterialCardView" -> SkinnableBydCardView(context, attrs)

            else -> super.createSkinnableView(name, context, attrs)
        }
    }
}
