package com.kylin.skinlibrary.utils

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * 本地键值存储工具（Tencent MMKV）。
 *
 * 替代原 PreferencesUtils（Jetpack DataStore Preferences）：
 * - MMKV 基于 mmap，读写纳秒/微秒级、同步 API、进程级单例，UI 线程直接调用无 IO 卡顿，
 *   因此不再需要协程（原 DataStore 的 suspend 读写 + Flow 订阅一并移除）；
 * - 进程级单例由 [MMKV.defaultMMKV] 保证，多个 Context 返回同一实例。
 *
 * 换肤库用它在宿主侧持久化「上次皮肤状态」（key 如 `currentSkin`）。
 * 键统一为 String 类型；值与类型一一对应，同一 key 勿混用不同类型读写。
 *
 * 用法：在 `Application.onCreate` 最早时机调用一次 [init]，之后即可随处 [putString] / [getString]。
 */
object KvStore {

    /**
     * 旧 SharedPreferences 名：DataStore 版之前的存储，也是 DataStore 曾通过
     * `SharedPreferencesMigration` 迁移的来源。SP 文件在 DataStore 迁移后并未被删除，
     * 仍保留历史值，故一次性 [init] 时从它 import 即可恢复旧值，无需再依赖 DataStore。
     */
    private const val LEGACY_SP_NAME = "com.netease.skin"

    @Volatile
    private var kv: MMKV? = null

    /**
     * 初始化（幂等）：MMKV 全进程只初始化一次，重复调用直接复用既有实例。
     * 首次初始化会把旧 SharedPreferences 的历史值一次性迁移进 MMKV。
     */
    @Synchronized
    fun init(context: Context) {
        if (kv != null) return
        val appContext = context.applicationContext
        MMKV.initialize(appContext)
        val store = MMKV.defaultMMKV()
        // 一次性迁移：import 旧 SharedPreferences（com.netease.skin）中的历史值。
        // 仅在首次 init 时执行，之后 kv 非空直接返回，不会覆盖运行期新写入的值。
        store.importFromSharedPreferences(
            appContext.getSharedPreferences(LEGACY_SP_NAME, Context.MODE_PRIVATE)
        )
        kv = store
    }

    private fun store(): MMKV = kv ?: throw IllegalStateException(
        "KvStore 未初始化，请先在 Application.onCreate 调用 KvStore.init(context)"
    )

    // ==================== 写（同步，UI 线程安全） ====================

    fun putString(key: String, value: String?) {
        // MMKV 不支持存 null 值：传 null 视为删除该 key（与旧 SharedPreferences/DataStore 行为对齐）。
        if (value == null) store().removeValueForKey(key) else store().encode(key, value)
    }

    fun putInt(key: String, value: Int) = store().encode(key, value)

    fun putLong(key: String, value: Long) = store().encode(key, value)

    fun putFloat(key: String, value: Float) = store().encode(key, value)

    fun putBoolean(key: String, value: Boolean) = store().encode(key, value)

    // ==================== 读（同步，一次性） ====================

    fun getString(key: String, default: String? = null): String? = store().decodeString(key, default)

    fun getInt(key: String, default: Int = 0): Int = store().decodeInt(key, default)

    fun getLong(key: String, default: Long = 0L): Long = store().decodeLong(key, default)

    fun getFloat(key: String, default: Float = 0f): Float = store().decodeFloat(key, default)

    fun getBoolean(key: String, default: Boolean = false): Boolean = store().decodeBool(key, default)

    // ==================== 工具 ====================

    /** 是否已存在该 key。 */
    fun contains(key: String): Boolean = store().containsKey(key)

    /** 删除一个或多个 key。 */
    fun remove(vararg keys: String) {
        keys.forEach { store().removeValueForKey(it) }
    }

    /** 清空全部键值。 */
    fun clear() = store().clearAll()
}
