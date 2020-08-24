package com.kylin.skinlibrary.beans

import android.content.res.Resources

class SkinCache(// 用于加载皮肤包资源
    val skinResources: Resources, // 皮肤包资源所在包名（注：皮肤包不在app内，也不限包名）
    val skinPackageName: String
)