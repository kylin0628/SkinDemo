package com.kylin.skinlibrary

/**
 * 皮肤资源类型开关：初始化时决定哪些资源类型参与换肤。
 *
 * 在 [SkinManager.init] 时传入；不设置则默认**仅颜色资源**参与换肤（`supportColor = true`，
 * 图片 / 字符串 / 尺寸默认关闭）。宿主如需其他类型，显式传入对应开关即可。
 * 配置仅在初始化时生效一次，运行期不可变，避免切换过程中状态不一致。
 *
 * @property supportDrawable 图片资源（drawable / mipmap）是否换肤
 * @property supportColor    颜色资源（color / colorStateList）是否换肤
 * @property supportString   字符串资源（string）是否换肤
 * @property supportDimen    尺寸资源（dimen）是否换肤
 */
data class SkinConfig(
    val supportDrawable: Boolean = false,
    val supportColor: Boolean = true,
    val supportString: Boolean = false,
    val supportDimen: Boolean = false,
)
