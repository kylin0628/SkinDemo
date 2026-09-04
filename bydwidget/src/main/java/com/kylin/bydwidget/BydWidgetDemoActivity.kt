package com.kylin.bydwidget

import android.os.Bundle
import com.kylin.skinlibrary.SkinUiHost

/**
 * 比亚迪官方控件演示页。
 *
 * **继承 [BydSkinActivity] 走主题库标准换肤链路**：基类已把 XML 标签替换成 SkinnableByd*
 * （继承比亚迪控件 + 实现 ViewsMatch），切肤时由 SkinActivity 的 `applyViews(decorView)` 统一
 * 遍历调用 `skinnableView()` 换肤，与原生 Skinnable* 控件同一条链路。业务 Activity 零换肤代码。
 *
 * 换肤颜色契约全部下沉到 `activity_byd_widget_demo.xml`：各控件的 textColor / progressTint /
 * cardBackgroundColor 等语义色在 XML 里声明，SkinnableByd*.skinnableView() 按名映射到皮肤包
 * 同名颜色，切肤时自动跟随，不在此处硬编码。
 */
class BydWidgetDemoActivity : BydSkinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_byd_widget_demo)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 全局主题切换悬浮按钮：比亚迪页同样走宿主实现切肤
        SkinUiHost.installThemeSwitcher?.invoke(this)
    }
}
