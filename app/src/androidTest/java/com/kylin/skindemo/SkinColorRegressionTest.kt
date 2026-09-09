package com.kylin.skindemo

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.views.SkinnableTextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 回归测试：锁定「显式传资源 ID 设色」的新契约，防止回退到「从 ARGB 反推资源」的串色 bug。
 *
 * ## 背景
 * 旧实现为「业务零改动」，用 `setTextColor(context.getColor(R.color.x))` 的**调用顺序**反推
 * 资源 ID（ThreadLocal 记录最近一次 getColor 的资源）。但 `setTextColor(int)` 的参数是 ARGB
 * 而非资源 ID，一旦两个不同资源解析出同一 ARGB（宿主同值），从 ARGB 反推在信息论上不可判定，
 * 「先批量取色、后设色」时必然串色（见 SkinColorCrossTalkDemo）。
 *
 * 新契约：删除 ARGB 反推，改用 [SkinnableTextView.setTextColorRes] **显式传 @ColorRes**，
 * 资源 ID 从调用方直达 attrsBean，不经过任何反推，同值资源也绝不串色。运行时用系统
 * `setTextColor(color)` 设色的控件不再自动换肤（这是删除反推的明确取舍）。
 *
 * ## 复现素材（app/values/colors.xml 与 skinpackage/values/colors.xml）
 * 宿主：text_hint = #99000000、text_secondary = #99000000（同值）
 * 皮肤：text_hint = #8D8D91、text_secondary = #ACACAF（不同值）
 */
@RunWith(AndroidJUnit4::class)
class SkinColorRegressionTest {

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
        SkinManager.instance?.loadSkin(null) // 干净起点：默认皮肤
    }

    @After
    fun tearDown() {
        SkinManager.instance?.loadSkin(null)
    }

    /** 皮肤包是否可用（未拷贝则跳过依赖它的断言，避免环境问题误报）。 */
    private fun skinReady(): Boolean {
        SkinManager.instance?.loadSkin(skinPath)
        return SkinManager.instance?.isDefaultSkin == false
    }

    /**
     * 换肤链路基础：两个「宿主同值、皮肤不同值」的资源，切动态皮肤后各自映射到不同色。
     * 证明同值色在「按名映射」这条链路上天然不冲突——宿主同值是常态，皮肤侧按名给出不同值即可。
     */
    @Test
    fun sameValueColors_mapToDistinctSkinColors() {
        if (!skinReady()) {
            println("WARN: 皮肤包未就绪，跳过动态皮肤断言")
            return
        }
        val manager = SkinManager.instance!!

        // 前置假设：默认皮肤下两者同值
        SkinManager.instance!!.loadSkin(null)
        val dHint = manager.getColor(hostTextHint)
        val dSecondary = manager.getColor(hostTextSecondary)
        assertTrue("前置假设不成立：宿主应存在同值色 text_hint/text_secondary",
            dHint == dSecondary)

        // 动态皮肤下两者各取各的
        manager.loadSkin(skinPath)
        val sHint = manager.getColor(hostTextHint)
        val sSecondary = manager.getColor(hostTextSecondary)
        assertNotEquals("动态皮肤下 text_hint 与 text_secondary 应不同", sHint, sSecondary)
    }

    /**
     * 端到端：两个真实控件用 [SkinnableTextView.setTextColorRes] **显式传 id** 分别设
     * text_hint / text_secondary（宿主同值），切动态皮肤后两控件颜色应各取各的皮肤色，互不串色。
     *
     * 这是新契约的核心：资源 ID 直达 attrsBean，不经过「ARGB 反推」，同值资源也绝不串色。
     */
    @Test
    fun setTextColorRes_twoViews_sameValueColors_skinSwitchNoCrossTalk() {
        if (!skinReady()) {
            println("WARN: 皮肤包未就绪，跳过端到端断言")
            return
        }
        val manager = SkinManager.instance!!

        SkinManager.instance!!.loadSkin(null)
        val viewHint = SkinnableTextView(targetContext)
        val viewSecondary = SkinnableTextView(targetContext)

        // 显式传 @ColorRes 设色（新 API）
        viewHint.setTextColorRes(hostTextHint)
        viewSecondary.setTextColorRes(hostTextSecondary)

        // 切动态皮肤并触发换肤遍历
        manager.loadSkin(skinPath)
        viewHint.skinnableView()
        viewSecondary.skinnableView()

        val expectedHint = manager.getColor(hostTextHint)
        val expectedSecondary = manager.getColor(hostTextSecondary)

        assertEquals("text_hint 控件切肤后颜色应等于皮肤包 text_hint", expectedHint, viewHint.currentTextColor)
        assertEquals("text_secondary 控件切肤后颜色应等于皮肤包 text_secondary", expectedSecondary, viewSecondary.currentTextColor)
        assertNotEquals("两控件切肤后颜色应不同（不串色）", viewHint.currentTextColor, viewSecondary.currentTextColor)
    }

    /**
     * 反向约束：`setTextColorRes` 后即使紧接着 `getColor` 解析了别的资源，也不影响已回填的 id。
     * 证明新 API 不依赖任何「调用顺序」，彻底消除旧实现「同值 + 批量取色」的串色路径。
     */
    @Test
    fun setTextColorRes_immuneToSubsequentGetColor() {
        if (!skinReady()) {
            println("WARN: 皮肤包未就绪，跳过端到端断言")
            return
        }
        val manager = SkinManager.instance!!

        SkinManager.instance!!.loadSkin(null)
        val viewHint = SkinnableTextView(targetContext)
        val viewSecondary = SkinnableTextView(targetContext)

        // 旧 bug 的触发顺序：先设 text_hint，紧接着 getColor(text_secondary)「污染」最近记录
        viewHint.setTextColorRes(hostTextHint)
        manager.getColor(hostTextSecondary) // 旧实现会在这里覆盖 ThreadLocal

        viewSecondary.setTextColorRes(hostTextSecondary)

        manager.loadSkin(skinPath)
        viewHint.skinnableView()
        viewSecondary.skinnableView()

        assertEquals("text_hint 控件切肤后不应被 text_secondary 串色",
            manager.getColor(hostTextHint), viewHint.currentTextColor)
        assertEquals("text_secondary 控件切肤后颜色应等于皮肤包 text_secondary",
            manager.getColor(hostTextSecondary), viewSecondary.currentTextColor)
    }
}
