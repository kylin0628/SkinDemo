package com.kylin.skinlibrary.utils

import android.util.Log
import com.kylin.skinlibrary.BuildConfig

/**
 * skinlibrary 统一日志门面。
 *
 * 目的：把主题库分散的 `android.util.Log` 收敛到一处，统一 TAG 前缀 + 分级，便于出问题时用
 * `adb logcat | grep "[Skin]"` 一键捞全量换肤日志。
 *
 * 级别约定（由重到轻）：
 * - [e] error：换肤流程出错、异常回退（永远打印）
 * - [w] warn：降级/回退等需要关注的异常路径（永远打印）
 * - [i] info：关键状态切换（初始化 / 切肤 / 加载皮肤包）——线上默认打印，用于事故回溯
 * - [d] debug：过程细节（inflate / 遍历 / 映射命中）——仅 debug 开关开启时打印，避免生产热路径开销
 *
 * 开关：默认跟随 [BuildConfig.DEBUG]（debug 构建 true、release 构建 false），
 * 也可运行时用 [SkinLog.debugEnabled] 强制开启/关闭（线上排查时 `SkinLog.debugEnabled = true` 即可）。
 */
object SkinLog {
    private const val PREFIX = "[Skin]"

    /** 是否允许打印 debug 级别日志；i/w/e 不受此开关限制，永远打印。 */
    @Volatile
    var debugEnabled: Boolean = BuildConfig.DEBUG

    fun d(tag: String, msg: String) {
        if (debugEnabled) Log.d(PREFIX + tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(PREFIX + tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(PREFIX + tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(PREFIX + tag, msg, throwable)
    }
}
