package com.kylin.skinlibrary

import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.TypedValue

/**
 * 皮肤感知的 [Resources]：在「颜色 / 图片 / 字符串 / 尺寸」读取上按名映射到皮肤包，其余走宿主。
 *
 * 背景：Compose 的 `colorResource(id)` / `painterResource(id)` / `stringResource(id)` /
 * `dimensionResource(id)` 内部都是 `LocalContext.current.resources.getXxx(id)`，
 * 资源 ID 在进入 [Resources] 时仍然存在（不同于原生 `setTextColor(getColor())` 的 ID 丢失）。
 * 因此只要让 Activity 的 `getResources()` 返回本类（见 [com.netease.skin.library.base.SkinActivity]），
 * 这些 Compose 资源 API 就自动按当前皮肤取值，业务代码零改动，且 `LocalContext` 仍是 Activity，
 * 不会破坏 `LocalContext.current as ComponentActivity` 之类强转。
 *
 * 默认皮肤下所有方法走 `super`，行为与原生 [Resources] 完全一致，无回归风险。
 */
class SkinnableResources(private val host: Resources) :
    Resources(host.assets, host.displayMetrics, host.configuration) {

    private val manager: SkinManager?
        get() = SkinManager.instance

    // ===== 颜色 =====
    override fun getColor(id: Int): Int {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getColor(id) else super.getColor(id)
    }

    override fun getColor(id: Int, theme: Theme?): Int {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getColor(id) else super.getColor(id, theme)
    }

    override fun getColorStateList(id: Int): ColorStateList {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getColorStateList(id) else super.getColorStateList(id)
    }

    override fun getColorStateList(id: Int, theme: Theme?): ColorStateList {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getColorStateList(id) else super.getColorStateList(id, theme)
    }

    // ===== 图片 =====
    override fun getDrawable(id: Int): Drawable {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getDrawableOrMipMap(id) else super.getDrawable(id)
    }

    override fun getDrawable(id: Int, theme: Theme?): Drawable {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getDrawableOrMipMap(id) else super.getDrawable(id, theme)
    }

    // ===== 字符串 =====
    override fun getString(id: Int): String {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getString(id) else super.getString(id)
    }

    override fun getString(id: Int, vararg formatArgs: Any): String {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getString(id, *formatArgs) else super.getString(id, *formatArgs)
    }

    override fun getText(id: Int): CharSequence {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getText(id) else super.getText(id)
    }

    // ===== 尺寸 =====
    override fun getDimension(id: Int): Float {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getDimension(id) else super.getDimension(id)
    }

    override fun getDimensionPixelSize(id: Int): Int {
        val m = manager
        return if (m != null && !m.isDefaultSkin) m.getDimensionPixelSize(id) else super.getDimensionPixelSize(id)
    }

    // ===== painterResource 矢量路径支持 =====
    // painterResource 实现（ui-android 1.5.3）：先 res.getValue(id, value, true) 取资源路径后缀判 xml 与否，
    // 是 xml 则走 res.getXml(id) 解析 vector。故需 override 这两个方法，矢量 drawable 才能按名映射到皮肤包。
    override fun getValue(id: Int, outValue: TypedValue, resolveRefs: Boolean) {
        val m = manager
        if (m != null && !m.isDefaultSkin) {
            val skinRes = m.getSkinResourcesOrNull()
            if (skinRes != null) {
                val ids = m.resolveSkinId(id)
                if (ids != 0) {
                    skinRes.getValue(ids, outValue, resolveRefs)
                    return
                }
            }
        }
        super.getValue(id, outValue, resolveRefs)
    }

    override fun getXml(id: Int): XmlResourceParser {
        val m = manager
        if (m != null && !m.isDefaultSkin) {
            val skinRes = m.getSkinResourcesOrNull()
            if (skinRes != null) {
                val ids = m.resolveSkinId(id)
                if (ids != 0) {
                    return skinRes.getXml(ids)
                }
            }
        }
        return super.getXml(id)
    }
}
