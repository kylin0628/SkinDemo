package com.netease.skin.library.base

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View

/**
 * LayoutInflater.Factory2 责任链注册表。
 *
 * 背景：framework 的 `LayoutInflater.setFactory2` 只能设一次（`mFactorySet` 置 true 后再设抛
 * `IllegalStateException`），所以「多个库各自调 `LayoutInflaterCompat.setFactory2`」必然有一个失效或抛异常。
 *
 * 本注册表让所有「想靠 Factory 拦截控件」的库改成 **register 注册**，由 [SkinActivity] 作为唯一入口
 * 在 `onCreateView` 里按注册顺序逐个尝试，实现多库互不冲突的兼用。
 *
 * 接入约定（第三方库必须遵守，否则仍会互相影响）：
 * 1. 不要自行调用 `LayoutInflaterCompat.setFactory2`，改调 [register]。
 * 2. 每个 Factory 对「自己不关心的 View」必须返回 **null** 放行，而不是 `super.onCreateView(...)` 的结果，
 *    否则会吃掉责任链，排在后面的库永远收不到 View。
 * 3. 若多个库抢「同一类控件」，责任链上**先注册者优先**，后来者对该类控件静默失效（无法无损叠加）。
 */
object LayoutFactoryRegistry {
    private val factories = mutableListOf<LayoutInflater.Factory2>()

    /** 注册一个 Factory2。重复注册同一实例会去重。 */
    @Synchronized
    fun register(factory: LayoutInflater.Factory2) {
        if (!factories.contains(factory)) {
            factories.add(factory)
        }
    }

    /**
     * 便捷重载：注册一个 [LayoutInflater.Factory]（单参数版本），内部自动包装成 Factory2。
     * 与 [LayoutInflaterCompat.setFactory2] 的 Factory2Wrapper 行为一致。
     */
    @Synchronized
    fun register(factory: LayoutInflater.Factory) {
        register(object : LayoutInflater.Factory2 {
            override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
                factory.onCreateView(name, context, attrs)

            override fun onCreateView(
                parent: View?,
                name: String,
                context: Context,
                attrs: AttributeSet
            ): View? = factory.onCreateView(name, context, attrs)
        })
    }

    /** 快照遍历，避免遍历途中并发注册导致 ConcurrentModificationException。 */
    @Synchronized
    fun snapshot(): List<LayoutInflater.Factory2> = factories.toList()
}
