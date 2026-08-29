package com.kylin.skinlibrary.model

import android.content.res.TypedArray
import android.util.Log
import android.util.SparseIntArray

/**
 * 临时JavaBean对象，用于存储控件的key、value
 * 如：key:android:textColor, value:@Color/xxx
 *
 *
 * 思考：动态加载的场景，键值对是否存储SharedPreferences呢？
 */
class AttrsBean {
    private val resourcesMap: SparseIntArray

    /**
     * 储控件的key、value
     *
     * @param typedArray 控件属性的类型集合，如：background / textColor
     * @param styleable  自定义属性，参考value/attrs.xml
     */
    fun saveViewResource(typedArray: TypedArray, styleable: IntArray) {
        for (i in 0 until typedArray.length()) {
            val key = styleable[i]
            val resourceId = typedArray.getResourceId(i, DEFAULT_VALUE)
            resourcesMap.put(key, resourceId)
            Log.e(
                "tag",
                "value = " + resourceId + "   key = " + key + " ----typedArray。lengh:" + typedArray.length() + "  styleable:" + styleable.size
            )
        }
    }

    /**
     * 获取控件某属性的resourceId
     *
     * @param styleable 自定义属性，参考value/attrs.xml
     * @return 某控件某属性的resourceId
     */
    fun getViewResource(styleable: Int): Int {
        return resourcesMap[styleable]
    }

    /**
     * 更新控件某属性的 resourceId。
     *
     * 背景：业务代码运行时 `setBackgroundResource(resId)` 会覆盖 XML 里 `android:background` 的
     * 资源（如 SceneCardViewHolder 按横竖屏/TRACK 动态切卡片背景）。若 attrsBean 仍记录 inflate 时的
     * 旧资源 ID，切肤遍历 skinnableView() 会用旧值把背景"换回"来，导致动态背景偶现不随主题变化。
     * 故在 setBackgroundResource 时同步记录最新 resourceId，使切肤遍历始终作用于当前背景。
     */
    fun updateViewResource(styleable: Int, resourceId: Int) {
        resourcesMap.put(styleable, resourceId)
    }

    companion object {
        private const val DEFAULT_VALUE = -1
    }

    init {
        resourcesMap = SparseIntArray()
    }
}