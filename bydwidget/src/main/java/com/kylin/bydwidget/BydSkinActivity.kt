package com.kylin.bydwidget

import com.kylin.bydwidget.views.SkinnableBydButton
import com.kylin.bydwidget.views.SkinnableBydCardView
import com.kylin.bydwidget.views.SkinnableBydEditText
import com.kylin.bydwidget.views.SkinnableBydProgressBar
import com.kylin.bydwidget.views.SkinnableBydSeekBar
import com.kylin.bydwidget.views.SkinnableBydSwitch
import com.kylin.bydwidget.views.SkinnableBydTextView
import com.kylin.skinlibrary.utils.SystemViewName
import com.netease.skin.library.base.SkinActivity
import com.netease.skin.library.base.SkinnableViewBinder

/**
 * 比亚迪控件换肤页基类。
 *
 * 把「XML 标签名 → SkinnableByd*」的映射从业务 Activity 下沉到本基类，并以**控件绑定列表**方式
 * 接入主题库：[SkinActivity.registerSkinnableViews] 传入一组 [SkinnableViewBinder]（控件类 + 标签名），
 * 标准控件（TextView/Button/EditText/ProgressBar/SeekBar/Switch/MaterialCardView）即自动替换成
 * 比亚迪皮肤感知控件，走 [SkinActivity] 统一的 `applyViews → skinnableView()` 换肤链路。
 *
 * 业务页只需 `class XxxActivity : BydSkinActivity()`，零换肤代码、零工厂重写。
 *
 * 绑定对「不关心的控件」返回 null，回落 [SkinActivity.createSkinnableView] 的内置原生映射，
 * 保证页面里其它普通控件仍走原生换肤。
 */
abstract class BydSkinActivity : SkinActivity() {

    /** 比亚迪换肤控件绑定列表：每条 = 控件类构造器引用 + 其接管的 XML 标签名集合 */
    private val bydSkinnableViewBinders: List<SkinnableViewBinder> = listOf(
        SkinnableViewBinder(
            setOf(SystemViewName.TEXT_VIEW, SystemViewName.ANDROID_TEXT_VIEW),
            ::SkinnableBydTextView
        ),
        SkinnableViewBinder(
            setOf(SystemViewName.BUTTON, SystemViewName.ANDROID_BUTTON, SystemViewName.APPCOMPAT_BUTTON),
            ::SkinnableBydButton
        ),
        SkinnableViewBinder(
            setOf(SystemViewName.EDIT_TEXT, SystemViewName.ANDROID_EDIT_TEXT, SystemViewName.TEXT_INPUT_EDIT_TEXT),
            ::SkinnableBydEditText
        ),
        SkinnableViewBinder(
            setOf(SystemViewName.PROGRESS_BAR, SystemViewName.ANDROID_PROGRESS_BAR),
            ::SkinnableBydProgressBar
        ),
        SkinnableViewBinder(
            setOf(SystemViewName.SEEK_BAR, SystemViewName.ANDROID_SEEK_BAR, SystemViewName.APPCOMPAT_SEEK_BAR),
            ::SkinnableBydSeekBar
        ),
        SkinnableViewBinder(
            setOf(SystemViewName.SWITCH, SystemViewName.ANDROID_SWITCH),
            ::SkinnableBydSwitch
        ),
        SkinnableViewBinder(
            setOf(SystemViewName.MATERIAL_CARD_VIEW),
            ::SkinnableBydCardView
        ),
    )

    init {
        registerSkinnableViews(bydSkinnableViewBinders)
    }
}
