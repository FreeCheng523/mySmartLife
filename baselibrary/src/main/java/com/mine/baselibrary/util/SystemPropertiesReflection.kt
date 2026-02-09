package com.mine.baselibrary.util

import android.util.Log
import java.lang.reflect.Method

/**
 * 使用反射调用SystemProperty的静态方法
 * 用于在无法直接访问SystemProperty类时获取系统属性
 */
object SystemPropertiesReflection {
    
    private const val TAG = "SystemPropertiesReflection"
    private const val SYSTEM_PROPERTIES_CLASS = "android.os.SystemProperties"
    
    private var systemPropertiesClass: Class<*>? = null
    private var getIntMethod: Method? = null
    private var getStringMethod: Method? = null
    private var getBooleanMethod: Method? = null
    private var getLongMethod: Method? = null
    private var getFloatMethod: Method? = null
    private var getDoubleMethod: Method? = null
    private var setMethod: Method? = null
    
    init {
        initializeReflection()
    }
    
    /**
     * 初始化反射相关的方法和类
     */
    private fun initializeReflection() {
        try {
            systemPropertiesClass = Class.forName(SYSTEM_PROPERTIES_CLASS)
            
            // 获取getInt方法
            getIntMethod = systemPropertiesClass?.getMethod("getInt", String::class.java, Int::class.javaPrimitiveType)
            
            // 获取getString方法
            getStringMethod = systemPropertiesClass?.getMethod("get", String::class.java)
            systemPropertiesClass?.getMethod("get", String::class.java, String::class.java)?.let {
                getStringMethod = it
            }
            
            // 获取getBoolean方法
            getBooleanMethod = systemPropertiesClass?.getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
            
            // 获取getLong方法
            getLongMethod = systemPropertiesClass?.getMethod("getLong", String::class.java, Long::class.javaPrimitiveType)
            
            // 获取getFloat方法（如果存在）
            try {
                getFloatMethod = systemPropertiesClass?.getMethod("getFloat", String::class.java, Float::class.javaPrimitiveType)
            } catch (e: NoSuchMethodException) {
                // getFloat方法可能不存在，忽略
            }
            
            // 获取getDouble方法（如果存在）
            try {
                getDoubleMethod = systemPropertiesClass?.getMethod("getDouble", String::class.java, Double::class.javaPrimitiveType)
            } catch (e: NoSuchMethodException) {
                // getDouble方法可能不存在，忽略
            }
            
            // 获取set方法
            setMethod = systemPropertiesClass?.getMethod("set", String::class.java, String::class.java)
            
            Log.d(TAG, "SystemProperties reflection initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SystemProperties reflection", e)
        }
    }
    
    /**
     * 获取系统属性整数值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            getIntMethod?.invoke(null, key, defaultValue) as? Int ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get int property: $key", e)
            defaultValue
        }
    }
    
    /**
     * 获取系统属性字符串值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            val method = if (getStringMethod?.parameterCount == 2) {
                getStringMethod
            } else {
                systemPropertiesClass?.getMethod("get", String::class.java, String::class.java)
            }
            method?.invoke(null, key, defaultValue) as? String ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get string property: $key", e)
            defaultValue
        }
    }
    
    /**
     * 获取系统属性字符串值（无默认值版本）
     * @param key 属性键
     * @return 属性值或空字符串
     */
    fun getString(key: String): String {
        return try {
            val method = systemPropertiesClass?.getMethod("get", String::class.java)
            method?.invoke(null, key) as? String ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get string property: $key", e)
            ""
        }
    }
    
    /**
     * 获取系统属性布尔值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            getBooleanMethod?.invoke(null, key, defaultValue) as? Boolean ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get boolean property: $key", e)
            defaultValue
        }
    }
    
    /**
     * 获取系统属性长整型值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            getLongMethod?.invoke(null, key, defaultValue) as? Long ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get long property: $key", e)
            defaultValue
        }
    }
    
    /**
     * 获取系统属性浮点值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    fun getFloat(key: String, defaultValue: Float = 0.0f): Float {
        return try {
            if (getFloatMethod != null) {
                getFloatMethod?.invoke(null, key, defaultValue) as? Float ?: defaultValue
            } else {
                // 如果getFloat方法不存在，尝试从字符串解析
                val stringValue = getString(key)
                if (stringValue.isNotEmpty()) {
                    stringValue.toFloatOrNull() ?: defaultValue
                } else {
                    defaultValue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get float property: $key", e)
            defaultValue
        }
    }
    
    /**
     * 获取系统属性双精度浮点值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return try {
            if (getDoubleMethod != null) {
                getDoubleMethod?.invoke(null, key, defaultValue) as? Double ?: defaultValue
            } else {
                // 如果getDouble方法不存在，尝试从字符串解析
                val stringValue = getString(key)
                if (stringValue.isNotEmpty()) {
                    stringValue.toDoubleOrNull() ?: defaultValue
                } else {
                    defaultValue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get double property: $key", e)
            defaultValue
        }
    }
    
    /**
     * 设置系统属性（String类型）
     * @param key 属性键
     * @param value 属性值
     * @return 是否设置成功
     */
    fun set(key: String, value: String): Boolean {
        return try {
            setMethod?.invoke(null, key, value)
            Log.d(TAG, "Set property: $key = $value")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set property: $key = $value", e)
            false
        }
    }
    
    /**
     * 设置系统属性（Int类型）
     * @param key 属性键
     * @param value 属性值
     * @return 是否设置成功
     */
    fun setInt(key: String, value: Int): Boolean {
        return set(key, value.toString())
    }
    
    /**
     * 设置系统属性（Boolean类型）
     * @param key 属性键
     * @param value 属性值
     * @return 是否设置成功
     */
    fun setBoolean(key: String, value: Boolean): Boolean {
        return set(key, value.toString())
    }
    
    /**
     * 设置系统属性（Long类型）
     * @param key 属性键
     * @param value 属性值
     * @return 是否设置成功
     */
    fun setLong(key: String, value: Long): Boolean {
        return set(key, value.toString())
    }
    
    /**
     * 设置系统属性（Float类型）
     * @param key 属性键
     * @param value 属性值
     * @return 是否设置成功
     */
    fun setFloat(key: String, value: Float): Boolean {
        return set(key, value.toString())
    }
    
    /**
     * 设置系统属性（Double类型）
     * @param key 属性键
     * @param value 属性值
     * @return 是否设置成功
     */
    fun setDouble(key: String, value: Double): Boolean {
        return set(key, value.toString())
    }
    
    /**
     * 检查反射是否可用
     * @return 是否可用
     */
    fun isReflectionAvailable(): Boolean {
        return systemPropertiesClass != null && 
               getIntMethod != null && 
               getStringMethod != null && 
               getBooleanMethod != null &&
               setMethod != null
    }
    
    /**
     * 获取反射状态信息
     * @return 状态信息字符串
     */
    fun getReflectionStatus(): String {
        return buildString {
            appendLine("SystemProperties Reflection Status:")
            appendLine("  Class loaded: ${systemPropertiesClass != null}")
            appendLine("  getInt method: ${getIntMethod != null}")
            appendLine("  getString method: ${getStringMethod != null}")
            appendLine("  getBoolean method: ${getBooleanMethod != null}")
            appendLine("  getLong method: ${getLongMethod != null}")
            appendLine("  getFloat method: ${getFloatMethod != null}")
            appendLine("  getDouble method: ${getDoubleMethod != null}")
            appendLine("  set method: ${setMethod != null}")
        }
    }
}
