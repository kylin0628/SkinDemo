package com.kylin.skinlibrary.beans

import android.content.res.TypedArray
import android.util.SparseIntArray

/**
 *@Description:
 *@Auther: wangqi
 * CreateTime: 2020/8/6.
 */
class AttrsBean {

    companion object {
        const val DEFAULT_ID = -1
    }

    val resourcesMap: SparseIntArray by lazy { SparseIntArray() }

    /**
     * 把加载的所有控件我们需要参与主题变化的属性创建对应的AttrsBean保存起来，便于后面切换主题的时候使用
     * typedArray和styleable长度一定是一致的，因为我们typedArray也是通过styleable得到的
     */
    fun saveViewResource(typedArray: TypedArray, styleable: IntArray) {
        for (index in 0 until typedArray.length()) {
            val key = styleable[index]
            val resourcesId = typedArray.getResourceId(index, DEFAULT_ID)
            resourcesMap.put(key, resourcesId)
        }
    }

    /**
     *  styleable 我们再attrs.xml中定义的系统组件我们需要参与的skin属性，比如android:textcolor属性
     *  return 某控件某属性的resourceId
     */
    fun getViewResource(styleable: Int) = resourcesMap.get(styleable)
}