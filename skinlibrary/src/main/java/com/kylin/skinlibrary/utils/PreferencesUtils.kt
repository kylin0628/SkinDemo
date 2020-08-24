/**
 * Project Name:SimonFramework
 * File Name:PreferencesUtils.java
 * Package Name:com.simon.framework.utils
 * Date:2016-5-19 下午2:24:22
 * Copyright (c) 2016, simon@cmonbaby.com All Rights Reserved.
 */
package com.kylin.skinlibrary.utils

import android.content.Context

/**
 * **自定义Preferences存储器工具类**
 *
 * **Preference Name**
 *  * you can change preference name by [.PREFERENCE_NAME]
 *
 *
 * **Put Value**
 *  * put string [.putString]
 *  * put int [.putInt]
 *  * put long [.putLong]
 *  * put float [.putFloat]
 *  * put boolean [.putBoolean]
 *
 *
 * **Get Value**
 *  * get string [.getString], [.getString]
 *  * get int [.getInt], [.getInt]
 *  * get long [.getLong], [.getLong]
 *  * get float [.getFloat], [.getFloat]
 *  * get boolean [.getBoolean], [.getBoolean]
 *
 *
 * **Other Opration**
 *  * remove String...keys [.removeSomeThing]
 *  * clearSP [.clearSP]
 *  * contains [.contains]
 *  * getAll [.getAll]
 *
 *
 * **Important Usage**
 *  * put [.put]
 *  * get [.get]
 *
 *
 * @Title PreferencesUtils
 * @Package com.cmonbaby.framework.utils
 * @Description 自定义Preferences存储器工具类
 * @author simon
 * @date 2016-5-19 下午2:24:22
 * @since JDK1.8 SDK6.0.1
 * @version V2.3.4
 */
object PreferencesUtils {
    var PREFERENCE_NAME = "com.netease.skin"

    /**
     * put string preferences
     *
     * @param context
     * @param key The name of the preference to modify
     * @param value The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    fun putString(context: Context, key: String?, value: String?): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(key, value)
        return editor.commit()
    }

    /**
     * get string preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @return The preference value if it exists, or "". Throws ClassCastException if there is a preference with this
     * name that is not a string
     * @see .getString
     */
    fun getString(context: Context, key: String?): String? {
        return getString(context, key, "")
    }

    /**
     * get string preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a string
     */
    fun getString(context: Context, key: String?, defaultValue: String?): String? {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.getString(key, defaultValue)
    }

    /**
     * put int preferences
     *
     * @param context
     * @param key The name of the preference to modify
     * @param value The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    fun putInt(context: Context, key: String?, value: Int): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putInt(key, value)
        return editor.commit()
    }

    /**
     * get int preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @return The preference value if it exists, or -1. Throws ClassCastException if there is a preference with this
     * name that is not a int
     * @see .getInt
     */
    fun getInt(context: Context, key: String?): Int {
        return getInt(context, key, -1)
    }

    /**
     * get int preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a int
     */
    fun getInt(context: Context, key: String?, defaultValue: Int): Int {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.getInt(key, defaultValue)
    }

    /**
     * put long preferences
     *
     * @param context
     * @param key The name of the preference to modify
     * @param value The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    fun putLong(context: Context, key: String?, value: Long): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putLong(key, value)
        return editor.commit()
    }

    /**
     * get long preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @return The preference value if it exists, or -1. Throws ClassCastException if there is a preference with this
     * name that is not a long
     * @see .getLong
     */
    fun getLong(context: Context, key: String?): Long {
        return getLong(context, key, -1)
    }

    /**
     * get long preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a long
     */
    fun getLong(context: Context, key: String?, defaultValue: Long): Long {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.getLong(key, defaultValue)
    }

    /**
     * put float preferences
     *
     * @param context
     * @param key The name of the preference to modify
     * @param value The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    fun putFloat(context: Context, key: String?, value: Float): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putFloat(key, value)
        return editor.commit()
    }

    /**
     * get float preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @return The preference value if it exists, or -1. Throws ClassCastException if there is a preference with this
     * name that is not a float
     * @see .getFloat
     */
    fun getFloat(context: Context, key: String?): Float {
        return getFloat(context, key, -1f)
    }

    /**
     * get float preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a float
     */
    fun getFloat(context: Context, key: String?, defaultValue: Float): Float {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.getFloat(key, defaultValue)
    }

    /**
     * put boolean preferences
     *
     * @param context
     * @param key The name of the preference to modify
     * @param value The new value for the preference
     * @return True if the new values were successfully written to persistent storage.
     */
    fun putBoolean(context: Context, key: String?, value: Boolean): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putBoolean(key, value)
        return editor.commit()
    }

    /**
     * get boolean preferences, default is false
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @return The preference value if it exists, or false. Throws ClassCastException if there is a preference with this
     * name that is not a boolean
     * @see .getBoolean
     */
    fun getBoolean(context: Context, key: String?): Boolean {
        return getBoolean(context, key, false)
    }

    /**
     * get boolean preferences
     *
     * @param context
     * @param key The name of the preference to retrieve
     * @param defaultValue Value to return if this preference does not exist
     * @return The preference value if it exists, or defValue. Throws ClassCastException if there is a preference with
     * this name that is not a boolean
     */
    fun getBoolean(context: Context, key: String?, defaultValue: Boolean): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.getBoolean(key, defaultValue)
    }

    /**
     * get all key and values
     * @param context
     * @return Map of all key and values
     */
    fun getAll(context: Context): Map<String, *> {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.all
    }

    /**
     * check sp's key already exists if save
     * @param context
     * @param key The name of the preference to retrieve
     * @return true is exists
     */
    fun contains(context: Context, key: String?): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return settings.contains(key)
    }

    /**
     * remove SomeThing from preferences
     *
     * @param context context
     * @param keys keys The name of the preference to retrieve
     */
    fun removeSomeThing(context: Context, vararg keys: String?): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        if (keys == null) return false
        for (k in keys) {
            editor.remove(k)
        }
        editor.commit()
        return true
    }

    /**
     * clear all params from preferences
     * @param context context
     * @return True if the clear values were successfully.
     */
    fun clearSP(context: Context): Boolean {
        val settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.clear()
        editor.commit()
        return true
    }

    /**
     * Methods to save data, we need to get the specific type of the preservation of the data,
     * and then call different methods based on type
     *
     * @param context context
     * @param key The need to store the key
     * @param object The need to store the value
     */
    fun put(context: Context, key: String?, `object`: Any) {
        if (`object` is String) {
            putString(context, key, `object`)
        } else if (`object` is Int) {
            putInt(context, key, `object`)
        } else if (`object` is Boolean) {
            putBoolean(context, key, `object`)
        } else if (`object` is Float) {
            putFloat(context, key, `object`)
        } else if (`object` is Long) {
            putLong(context, key, `object`)
        } else {
            putString(context, key, `object`.toString())
        }
    }

    /**
     * Methods to save the data, we get the specific type of the data saved according to the default values,
     * and then call the method to obtain the value of the method
     *
     * @param context context
     * @param key The need to obtain the key
     * @param defaultObject if obtain nothing, input default value
     * @return if nothing return the result was null
     */
    operator fun get(context: Context, key: String?, defaultObject: Any?): Any? {
        if (defaultObject is String) {
            return getString(context, key, defaultObject as String?)
        } else if (defaultObject is Int) {
            return getInt(context, key, defaultObject)
        } else if (defaultObject is Boolean) {
            return getBoolean(context, key, defaultObject)
        } else if (defaultObject is Float) {
            return getFloat(context, key, defaultObject)
        } else if (defaultObject is Long) {
            return getLong(context, key, defaultObject)
        }
        return null
    }
}