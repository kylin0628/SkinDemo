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

    companion object {
        private const val DEFAULT_VALUE = -1
    }

    init {
        resourcesMap = SparseIntArray()
    }
}