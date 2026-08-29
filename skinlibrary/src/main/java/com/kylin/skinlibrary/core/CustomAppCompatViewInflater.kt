package com.kylin.skinlibrary.core

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatViewInflater
import com.kylin.skinlibrary.utils.SystemViewName
import com.kylin.skinlibrary.views.*

/**
 * 自定义控件加载器（可以考虑该类不被继承）
 */
class CustomAppCompatViewInflater(  // 上下文
    private val context: Context
) : AppCompatViewInflater() {
    private var name // 控件名
            : String? = null
    private var attrs // 某控件对应所有属性
            : AttributeSet? = null

    fun setName(name: String?) {
        this.name = name
    }

    fun setAttrs(attrs: AttributeSet?) {
        this.attrs = attrs
    }

    /**
     * @return 自动匹配控件名，并初始化控件对象
     */
    fun autoMatch(): View? {
        var view: View? = null
        when (name) {
            SystemViewName.NESTED_SCROLL_VIEW -> {
                // view = super.createTextView(context, attrs); // 源码写法
                view = SkinnableNestedScrollView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.CONSTRAINT_LAYOUT -> {
                // view = super.createTextView(context, attrs); // 源码写法
                view = SkinnableConstraintLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.LINEAR_LAYOUT -> {
                // view = super.createTextView(context, attrs); // 源码写法
                view = SkinnableLinearLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.LINEAR_LAYOUT_COMPAT -> {
                view = SkinnableLinearLayoutCompat(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.RELATIVE_LAYOUT -> {
                view = SkinnableRelativeLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TEXT_VIEW -> {
                view = SkinnableTextView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.IMAGE_VIEW -> {
                view = SkinnableImageView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.MATERIAL_BUTTON, SystemViewName.BUTTON -> {
                view = SkinnableButton(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.APPCOMPAT_EDIT_TEXT, SystemViewName.EDIT_TEXT -> {
                view = SkinnableEditText(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.FRAME_LAYOUT -> {
                view = SkinnableFrameLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.SCROLL_VIEW -> {
                view = SkinnableScrollView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.MATERIAL_CARD_VIEW, SystemViewName.CARD_VIEW -> {
                view = SkinnableCardView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.IMAGE_BUTTON -> {
                view = SkinnableImageButton(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.CHECK_BOX -> {
                view = SkinnableCheckBox(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.RADIO_BUTTON -> {
                view = SkinnableRadioButton(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.SWITCH_COMPAT, SystemViewName.SWITCH -> {
                view = SkinnableSwitchCompat(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TOGGLE_BUTTON -> {
                view = SkinnableToggleButton(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.CHECKED_TEXT_VIEW -> {
                view = SkinnableCheckedTextView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.APPCOMPAT_AUTO_COMPLETE_TEXT_VIEW, SystemViewName.AUTO_COMPLETE_TEXT_VIEW -> {
                view = SkinnableAutoCompleteTextView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TAB_LAYOUT -> {
                view = SkinnableTabLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.VIEW -> {
                view = SkinnableView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.RECYCLER_VIEW -> {
                view = SkinnableRecyclerView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.VIEW_PAGER -> {
                view = SkinnableViewPager(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.HORIZONTAL_SCROLL_VIEW -> {
                view = SkinnableHorizontalScrollView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.SHAPEABLE_IMAGE_VIEW -> {
                view = SkinnableShapeableImageView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TEXT_SWITCHER -> {
                view = SkinnableTextSwitcher(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.VIEW_SWITCHER -> {
                view = SkinnableViewSwitcher(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.VIEW_FLIPPER -> {
                view = SkinnableViewFlipper(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.PROGRESS_BAR -> {
                view = SkinnableProgressBar(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.SEEK_BAR -> {
                view = SkinnableSeekBar(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.RATING_BAR -> {
                view = SkinnableRatingBar(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.RADIO_GROUP -> {
                view = SkinnableRadioGroup(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.SPINNER -> {
                view = SkinnableSpinner(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.LIST_VIEW -> {
                view = SkinnableListView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.GRID_VIEW -> {
                view = SkinnableGridView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.EXPANDABLE_LIST_VIEW -> {
                view = SkinnableExpandableListView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.GRID_LAYOUT -> {
                view = SkinnableGridLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TABLE_LAYOUT -> {
                view = SkinnableTableLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.MULTI_AUTO_COMPLETE_TEXT_VIEW -> {
                view = SkinnableMultiAutoCompleteTextView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TEXT_CLOCK -> {
                view = SkinnableTextClock(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.COORDINATOR_LAYOUT -> {
                view = SkinnableCoordinatorLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.APP_BAR_LAYOUT -> {
                view = SkinnableAppBarLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.COLLAPSING_TOOLBAR_LAYOUT -> {
                view = SkinnableCollapsingToolbarLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.DRAWER_LAYOUT -> {
                view = SkinnableDrawerLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TOOLBAR -> {
                view = SkinnableToolbar(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.BOTTOM_NAVIGATION_VIEW -> {
                view = SkinnableBottomNavigationView(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.FLOATING_ACTION_BUTTON -> {
                view = SkinnableFloatingActionButton(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.CHIP -> {
                view = SkinnableChip(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.CHIP_GROUP -> {
                view = SkinnableChipGroup(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TEXT_INPUT_LAYOUT -> {
                view = SkinnableTextInputLayout(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.TEXT_INPUT_EDIT_TEXT -> {
                view = SkinnableTextInputEditText(context, attrs)
                verifyNotNull(view, name)
            }
            SystemViewName.MATERIAL_DIVIDER -> {
                view = SkinnableMaterialDivider(context, attrs)
                verifyNotNull(view, name)
            }
        }
        return view
    }

    /**
     * 校验控件不为空（源码方法，由于private修饰，只能复制过来了。为了代码健壮，可有可无）
     *
     * @param view 被校验控件，如：AppCompatTextView extends TextView（v7兼容包，兼容是重点！！！）
     * @param name 控件名，如："ImageView"
     */
    private fun verifyNotNull(view: View?, name: String?) {
        checkNotNull(view) { this.javaClass.name + " asked to inflate view for <" + name + ">, but returned null" }
    }
}