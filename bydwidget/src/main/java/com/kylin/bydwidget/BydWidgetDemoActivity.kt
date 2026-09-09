package com.kylin.bydwidget

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.byd.widget.BydAlertBuilder
import com.byd.widget.BydPopupItem
import com.byd.widget.BydPopupMenu
import com.byd.widget.BydTitleBar
import com.byd.widget.listItem.BydListItem
import com.byd.widget.listItem.BydSwitchListItem
import com.byd.widget.table.BydTabLayout
import com.byd.widget.sidebar.BydSideBar
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
 *
 * 展示型控件（TitleBar / SearchView / DatePicker / TimePicker / NumberPicker / ListItem /
 * SwitchListItem / TabLayout / SideBar / AlertDialog / PopupMenu）内部走 widget-tluc token 取色，
 * 只跟随系统 uiMode 暗黑/浅色，**不跟随动态主题色**，此处仅做展示与最小初始化。
 */
class BydWidgetDemoActivity : BydSkinActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_byd_widget_demo)
        initShowcaseControls()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 全局主题切换悬浮按钮：比亚迪页同样走宿主实现切肤
        SkinUiHost.installThemeSwitcher?.invoke(this)
    }

    /** 展示型控件最小初始化（标题/文案/标签/侧栏项等） */
    private fun initShowcaseControls() {
        findViewById<BydTitleBar>(R.id.byd_title_bar)?.setTitle("BydTitleBar 标题栏")

        findViewById<BydListItem>(R.id.byd_list_item)?.apply {
            setTitle("BydListItem 标题")
            setSummary("这是列表项摘要")
        }

        findViewById<BydSwitchListItem>(R.id.byd_switch_list_item)?.apply {
            setTitle("BydSwitchListItem 开关列表项")
        }

        findViewById<BydTabLayout>(R.id.byd_tab_layout)?.apply {
            addTab(newTab().setText("标签 1"))
            addTab(newTab().setText("标签 2"))
            addTab(newTab().setText("标签 3"))
        }

        findViewById<BydSideBar>(R.id.byd_side_bar)?.apply {
            for (title in listOf("A", "B", "C", "D")) {
                addBydSideBarTab(newBydSideBarTab().setTitle(title))
            }
        }

        findViewById<View>(R.id.btn_byd_alert_dialog)?.setOnClickListener { showBydAlertDialog() }
        findViewById<View>(R.id.btn_byd_popup_menu)?.setOnClickListener { showBydPopupMenu() }
    }

    /** 比亚迪告警弹框演示 */
    private fun showBydAlertDialog() {
        BydAlertBuilder(this)
            .setTitle("提示")
            .setMessage("这是比亚迪 BydAlertDialog 弹框")
            .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** 比亚迪弹出菜单演示 */
    private fun showBydPopupMenu() {
        val items = listOf(
            BydPopupItem("菜单项 1"),
            BydPopupItem("菜单项 2"),
            BydPopupItem("菜单项 3"),
        )
        val menu = BydPopupMenu(this, ArrayList(items)).apply {
            setChoiceIconVisible(true)
        }
        val anchor = findViewById<View>(R.id.btn_byd_popup_menu) ?: return
        menu.getPopupWindow().setOnItemClickListener { position ->
            Toast.makeText(this, "点击了 ${items[position].itemContent}", Toast.LENGTH_SHORT).show()
        }
        menu.getPopupWindow().showAsDropDown(anchor)
    }
}
