package com.kylin.skindemo

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.LayoutInflaterCompat
import androidx.fragment.app.DialogFragment
import com.kylin.skinlibrary.SkinManager
import com.kylin.skinlibrary.core.CustomAppCompatViewInflater
import com.netease.skin.library.base.SkinActivity

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
 */
class MultiLevelDialogFragment : DialogFragment(), LayoutInflater.Factory2 {

    companion object {
        private const val ARG_LEVEL = "level"
        fun newInstance(level: Int) = MultiLevelDialogFragment().apply {
            arguments = Bundle().apply { putInt(ARG_LEVEL, level) }
        }
    }

    private var viewInflater: CustomAppCompatViewInflater? = null

    private val level: Int
        get() = arguments?.getInt(ARG_LEVEL, 1) ?: 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dialogInflater = inflater.cloneInContext(requireContext())
        LayoutInflaterCompat.setFactory2(dialogInflater, this)
        return dialogInflater.inflate(R.layout.dialog_multi_level, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        if (activity is SkinActivity) activity.applyViews(view)
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
        view.findViewById<View>(R.id.btn_multi_dynamic)?.setOnClickListener {
            if (activity is SkinActivity) {
                activity.skinDynamic(skinPath, R.color.skin_item_color)
                com.kylin.skinlibrary.utils.PreferencesUtils.putString(requireContext(), "currentSkin", "skindemo")
            }
        }
        view.findViewById<View>(R.id.btn_multi_default)?.setOnClickListener {
            if (activity is SkinActivity) {
                activity.defaultSkin(R.color.colorPrimary)
                com.kylin.skinlibrary.utils.PreferencesUtils.putString(requireContext(), "currentSkin", "default")
            }
        }
        view.findViewById<View>(R.id.btn_multi_close)?.setOnClickListener { dismiss() }
    }

    // =================== Factory2 ===================

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (name == "fragment" || name == "androidx.fragment.app.FragmentContainerView") return null
        if (viewInflater == null) viewInflater = CustomAppCompatViewInflater(context)
        viewInflater!!.setName(name)
        viewInflater!!.setAttrs(attrs)
        return viewInflater!!.autoMatch()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
        onCreateView(null, name, context, attrs)
}
