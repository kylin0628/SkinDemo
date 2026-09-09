package com.kylin.skindemo

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.views.SkinnableTextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 演示「同一个颜色值、不同资源 id」在**新契约下不再串色**。
 *
 * ## 历史 bug（已修复）
 * 旧实现用「调用顺序」从 ARGB 反推资源 ID。宿主里 text_hint 与 text_secondary 同值 #99000000，
 * 业务「先批量取色、后设色」时，`setTextColor(titleColor)` 会把 text_hint 的色值误判成
 * text_secondary（因为同值校验失效），切肤后串色。
 *
 * ## 新契约
 * 删除 ARGB 反推，改用 [SkinnableTextView.setTextColorRes] 显式传 @ColorRes。资源 ID 直达
 * attrsBean，不经过反推，同值资源也绝不串色。本测试锁定这一行为。
 *
 * ## 复现素材
 * 宿主：text_hint = #99000000、text_secondary = #99000000（同值）
 * 皮肤：text_hint = #8D8D91、text_secondary = #ACACAF（不同值）
 */
@RunWith(AndroidJUnit4::class)
class SkinColorCrossTalkDemo {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val skinPath =
        "${targetContext.getExternalFilesDir("skindemo")!!.absolutePath}/skindemo.skin"

    private val hostTextHint =
        targetContext.resources.getIdentifier("text_hint", "color", targetContext.packageName)
    private val hostTextSecondary =
        targetContext.resources.getIdentifier("text_secondary", "color", targetContext.packageName)

    @Before
    fun setUp() {
        SkinManager.init(targetContext.applicationContext as Application)
        SkinManager.instance?.loadSkin(null)
    }

    @After
    fun tearDown() {
        SkinManager.instance?.loadSkin(null)
    }

    /**
     * 核心：setTextColorRes 显式传 id，即使「先设 text_hint 再 getColor(text_secondary)」——
     * 即旧 bug 的精确触发顺序——也不会串色。切肤后 titleView 取 text_hint 的皮肤色。
     */
    @Test
    fun setTextColorRes_sameValueColors_noCrossTalk() {
        val manager = SkinManager.instance!!

        // 前置：默认皮肤下两者同值 #99000000
        SkinManager.instance!!.loadSkin(null)
        val hintHost = manager.getColor(hostTextHint)
        val secondaryHost = manager.getColor(hostTextSecondary)
        println("[DEMO] 默认皮肤  text_hint=#${Integer.toHexString(hintHost)}  " +
            "text_secondary=#${Integer.toHexString(secondaryHost)}  → 同值: ${hintHost == secondaryHost}")

        val titleView = SkinnableTextView(targetContext)
        val subView = SkinnableTextView(targetContext)

        // 旧 bug 的精确触发顺序：设 text_hint 后紧接 getColor(text_secondary)
        titleView.setTextColorRes(hostTextHint)
        manager.getColor(hostTextSecondary)
        subView.setTextColorRes(hostTextSecondary)

        // 切动态皮肤，串色显形（若回填错 id，这里会串色）
        manager.loadSkin(skinPath)
        titleView.skinnableView()
        subView.skinnableView()

        val expectedHint = manager.getColor(hostTextHint)          // #8D8D91
        val expectedSecondary = manager.getColor(hostTextSecondary) // #ACACAF

        println("[DEMO] titleView  期望=#${Integer.toHexString(expectedHint)}  实际=#${Integer.toHexString(titleView.currentTextColor)}")
        println("[DEMO] subView   期望=#${Integer.toHexString(expectedSecondary)}  实际=#${Integer.toHexString(subView.currentTextColor)}")

        assertEquals("titleView 应取皮肤包 text_hint 色，不应被 text_secondary 串色",
            expectedHint, titleView.currentTextColor)
        assertEquals("subView 应取皮肤包 text_secondary 色",
            expectedSecondary, subView.currentTextColor)
    }

    /**
     * 明确取舍的锁定：系统 `setTextColor(color)` 设色的控件【不再自动换肤】。
     * 删除 ARGB 反推后，字面量色值切肤时保持当前色（不跟随皮肤）。这是「零串色」的代价，
     * 需要换肤的运行时设色必须改用 setTextColorRes。若此断言未来被误改回「自动跟随」，
     * 说明有人重新引入了反推机制。
     */
    @Test
    fun systemSetTextColor_doesNotFollowSkin() {
        val manager = SkinManager.instance!!

        SkinManager.instance!!.loadSkin(null)
        val view = SkinnableTextView(targetContext)
        val literal = 0xFF00FF00.toInt() // 字面量绿
        view.setTextColor(literal)

        manager.loadSkin(skinPath)
        view.skinnableView()

        // 切肤后字面量色保持不变（不跟随皮肤，也不被反推污染）
        assertEquals("系统 setTextColor 字面量色切肤后应保持不变", literal, view.currentTextColor)
    }
}
