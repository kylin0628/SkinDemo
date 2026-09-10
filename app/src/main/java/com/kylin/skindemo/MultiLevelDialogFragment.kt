package com.kylin.skindemo

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import com.netease.skin.library.base.SkinActivity
import com.netease.skin.library.base.SkinDialogFragment

/**
 * 多层弹框换肤案例。
 *
 * 每层 DialogFragment 内部含一个 RecyclerView/ListView 列表 + 三个按钮：
 * - 「下一层 DialogFragment」：叠加下一层弹框
 * - 「弹 PopupWindow」：在弹框内再弹 PopupWindow
 * - 「弹原生 Dialog」：在弹框内再弹原生 Dialog
 *
 * 用于验证 [SkinActivity.applyViewsToDialogs] 的多层递归遍历 + [SkinManager.registerWindow]
 * 独立窗口注册，切肤时所有层级的弹框与内嵌列表全部跟随换肤。
 *
 * 换肤样板已下沉到 [SkinDialogFragment] 基类，此处只保留弹框业务。
 */
class MultiLevelDialogFragment : SkinDialogFragment() {

    companion object {
        private const val ARG_LEVEL = "level"
        fun newInstance(level: Int) = MultiLevelDialogFragment().apply {
            arguments = Bundle().apply { putInt(ARG_LEVEL, level) }
        }
    }

    private val level: Int
        get() = arguments?.getInt(ARG_LEVEL, 1) ?: 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme)
    }

    override fun getLayoutResId(): Int = R.layout.dialog_multi_level

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 弹框内注入悬浮切肤入口（独立 Window 会遮挡 Activity 的悬浮按钮）
        (dialog?.window?.decorView as? FrameLayout)?.let {
            ThemeSwitcher.installFabInto(it, requireContext())
        }

        // 标题 + 层级
        view.findViewById<TextView>(R.id.tv_multi_title)?.text = "第 $level 层弹框"

        // 列表（复用主题色背景，验证列表 item 跟随换肤）
        // 关键：item 必须用带资源 ID 的布局（SkinnableTextView），否则切肤时颜色已丢失无法映射。
        val listView = view.findViewById<ListView>(R.id.lv_multi_list)
        val data = (1..20).map { "第 $level 层 · 列表项 $it" }
        listView.adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_skin_list,
            R.id.tv_item_text,
            data
        )

        // 下一层 DialogFragment
        view.findViewById<View>(R.id.btn_next_level)?.setOnClickListener {
            newInstance(level + 1).show(parentFragmentManager, "MultiLevel_${level + 1}")
        }

        // 弹 PopupWindow（嵌套独立窗口）
        view.findViewById<View>(R.id.btn_nested_popup)?.setOnClickListener { anchor ->
            PopupWindowDemo.show(requireContext(), anchor)
        }

        // 弹原生 Dialog（嵌套独立窗口）
        view.findViewById<View>(R.id.btn_nested_dialog)?.setOnClickListener {
            SkinTestDialog(requireContext()).showWithSkin()
        }

        // 切肤按钮（演示弹框内直接切肤）
        val skinPath = "${requireContext().getExternalFilesDir("skindemo")!!.absolutePath}/skindemo.skin"
        val activity = requireActivity()
        view.findViewById<View>(R.id.btn_multi_dynamic)?.setOnClickListener {
            if (activity is SkinActivity) {
                activity.skinDynamic(skinPath)
                SkinApp.persistCurrentSkin(requireContext(), "skindemo")
            }
        }
        view.findViewById<View>(R.id.btn_multi_default)?.setOnClickListener {
            if (activity is SkinActivity) {
                activity.defaultSkin()
                SkinApp.persistCurrentSkin(requireContext(), "default")
            }
        }
        view.findViewById<View>(R.id.btn_multi_close)?.setOnClickListener { dismiss() }
    }
}
