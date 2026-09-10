package com.kylin.skinlibrary

/**
 * 可换肤主题宿主契约。
 *
 * 抽象「能按皮肤包路径切换主题」的最小能力，供 [SkinUiHost.applyTheme] 这类宿主钩子
 * 依赖，避免钩子签名绑定具体实现类（继承版 [com.netease.skin.library.base.SkinActivity]
 * 与组合版 [com.netease.skin.library.base.SkinActivityDelegate] 都实现本接口）。
 *
 * 宿主 app 注册的「应用一套主题」统一策略只需调用这两个方法，无需关心具体是继承还是组合接入。
 */
interface SkinnableThemeHost {
    /** 切换到皮肤包路径（null = 默认皮肤）。 */
    fun skinDynamic(skinPath: String?)

    /** 切换回默认皮肤。 */
    fun defaultSkin()
}
