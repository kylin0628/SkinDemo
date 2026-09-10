package com.kylin.skinlibrary.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

/** DataStore 文件名（落到宿主 data 目录下 `<name>.preferences_pb`）。 */
private const val DATASTORE_NAME = "skindemo_preferences"

/** 旧 SharedPreferences 名：仅用于一次性迁移历史数据（见 [Context.skinDataStore]）。 */
private const val LEGACY_SP_NAME = "com.netease.skin"

/**
 * 进程级 DataStore 单例（Kotlin 属性委托保证每个进程只初始化一次，多个 Context 返回同一实例）。
 *
 * 使用 Jetpack DataStore Preferences 替代 SharedPreferences：
 * - 写为异步落盘（[edit] 内部走 IO 线程），不再像旧版 `.commit()` 那样在调用线程同步写磁盘；
 * - 读为 [Flow]，可订阅数据变化（[PreferencesUtils.data]）；
 * - 通过 [SharedPreferencesMigration] 自动迁移旧 SharedPreferences（`currentSkin`）历史值，升级不丢状态。
 */
val Context.skinDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_SP_NAME))
    }
)

/**
 * 本地键值存储工具（Jetpack DataStore Preferences）。
 *
 * 换肤库用它在宿主侧持久化「上次皮肤状态」（key 如 `currentSkin`）。
 * 所有读写均为 suspend（读也可经 [data] 以 Flow 订阅），调用方需持有协程作用域。
 * 键统一为 String 类型；值与类型一一对应，同一 key 勿混用不同类型读写。
 */
object PreferencesUtils {

    /** 数据流：供需要响应式订阅（如 Compose `collectAsState`）的调用方使用。 */
    fun data(context: Context): Flow<Preferences> = context.preferencesFlow()

    // ==================== 写（suspend） ====================

    suspend fun putString(context: Context, key: String, value: String?) {
        context.skinDataStore.edit { prefs ->
            val k = stringPreferencesKey(key)
            // DataStore 不支持存 null 值：传 null 视为删除该 key（与旧 SharedPreferences 行为对齐）。
            if (value == null) prefs.remove(k) else prefs[k] = value
        }
    }

    suspend fun putInt(context: Context, key: String, value: Int) {
        context.skinDataStore.edit { it[intPreferencesKey(key)] = value }
    }

    suspend fun putLong(context: Context, key: String, value: Long) {
        context.skinDataStore.edit { it[longPreferencesKey(key)] = value }
    }

    suspend fun putFloat(context: Context, key: String, value: Float) {
        context.skinDataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    suspend fun putBoolean(context: Context, key: String, value: Boolean) {
        context.skinDataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    // ==================== 读（suspend，一次性） ====================

    suspend fun getString(context: Context, key: String, default: String? = null): String? =
        context.preferencesFlow().first()[stringPreferencesKey(key)] ?: default

    suspend fun getInt(context: Context, key: String, default: Int = 0): Int =
        context.preferencesFlow().first()[intPreferencesKey(key)] ?: default

    suspend fun getLong(context: Context, key: String, default: Long = 0L): Long =
        context.preferencesFlow().first()[longPreferencesKey(key)] ?: default

    suspend fun getFloat(context: Context, key: String, default: Float = 0f): Float =
        context.preferencesFlow().first()[floatPreferencesKey(key)] ?: default

    suspend fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        context.preferencesFlow().first()[booleanPreferencesKey(key)] ?: default

    // ==================== 工具 ====================

    /** 一次性读取全部键值。 */
    suspend fun getAll(context: Context): Preferences = context.preferencesFlow().first()

    /** 是否已存在该 key（本工具键均为 String 类型）。 */
    suspend fun contains(context: Context, key: String): Boolean =
        context.preferencesFlow().first().contains(stringPreferencesKey(key))

    /** 删除一个或多个 key。 */
    suspend fun remove(context: Context, vararg keys: String) {
        context.skinDataStore.edit { prefs ->
            keys.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    /** 清空全部键值。 */
    suspend fun clear(context: Context) {
        context.skinDataStore.edit { it.clear() }
    }

    // ==================== 内部 ====================

    /** 数据流带损坏兜底：文件损坏（IOException）时回退空值，而非向上抛异常。 */
    private fun Context.preferencesFlow(): Flow<Preferences> =
        skinDataStore.data.catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences())
            else throw throwable
        }
}
