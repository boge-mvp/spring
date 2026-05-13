package com.boge.extend

import kotlinx.serialization.json.*

/**
 * 从JsonObject中获取指定键的字符串值，如果不存在或值为null，则返回默认值。
 *
 * @param key 需要获取值的键
 * @param defaultValue 当指定键不存在或者其对应的值为null时，返回的默认值
 * @return 指定键所对应的字符串值，若不存在则返回默认值
 */
fun JsonObject.optString(key: String, defaultValue: String = ""): String {
    return this.optStringOrNull(key) ?: defaultValue
}

/**
 * 从JsonObject中尝试获取指定键的字符串值，如果不存在或值为null，则返回null。
 *
 * @param key 需要获取值的键
 * @return 指定键所对应的字符串值，若不存在或值为null则返回null
 */
fun JsonObject.optStringOrNull(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
}

/**
 * 从JsonObject中获取指定键的整数值，如果不存在、值为null或无法转换为整数，则返回默认值。
 *
 * @param key 需要获取值的键
 * @param defaultValue 当指定键不存在、其对应的值为null或无法转换为整数时，返回的默认值
 * @return 指定键所对应的整数值，若不存在、值为null或无法转换为整数则返回默认值
 */
fun JsonObject.optInt(key: String, defaultValue: Int = 0): Int {
    return this.optIntOrNull(key) ?: defaultValue
}

/**
 * 从JsonObject中尝试获取指定键的整数值，如果不存在、值为null或无法转换为整数，则返回null。
 *
 * @param key 需要获取值的键
 * @return 指定键所对应的整数值，若不存在、值为null或无法转换为整数则返回null
 */
fun JsonObject.optIntOrNull(key: String): Int? {
    return this[key]?.jsonPrimitive?.intOrNull
}

/**
 * 从JsonObject中获取指定键的长整数值，如果不存在、值为null或无法转换为长整数，则返回默认值。
 *
 * @param key 需要获取值的键
 * @param defaultValue 当指定键不存在、其对应的值为null或无法转换为长整数时，返回的默认值
 * @return 指定键所对应的长整数值，若不存在、值为null或无法转换为长整数则返回默认值
 */
fun JsonObject.optLong(key: String, defaultValue: Long = 0): Long {
    return this.optLongOrNull(key) ?: defaultValue
}

/**
 * 从JsonObject中尝试获取指定键的长整数值，如果不存在、值为null或无法转换为长整数，则返回null。
 *
 * @param key 需要获取值的键
 * @return 指定键所对应的长整数值，若不存在、值为null或无法转换为长整数则返回null
 */
fun JsonObject.optLongOrNull(key: String): Long? {
    return this[key]?.jsonPrimitive?.longOrNull
}

/**
 * 从JsonObject中获取指定键的浮点数值，如果不存在、值为null或无法转换为浮点数，则返回默认值。
 *
 * @param key 需要获取值的键
 * @param defaultValue 当指定键不存在、其对应的值为null或无法转换为浮点数时，返回的默认值
 * @return 指定键所对应的浮点数值，若不存在、值为null或无法转换为浮点数则返回默认值
 */
fun JsonObject.optFloat(key: String, defaultValue: Float = 0.0f): Float {
    return this.optFloatOrNull(key) ?: defaultValue
}

/**
 * 从JsonObject中尝试获取指定键的浮点数值，如果不存在、值为null或无法转换为浮点数，则返回null。
 *
 * @param key 需要获取值的键
 * @return 指定键所对应的浮点数值，若不存在、值为null或无法转换为浮点数则返回null
 */
fun JsonObject.optFloatOrNull(key: String): Float? {
    return this[key]?.jsonPrimitive?.floatOrNull
}

/**
 * 从JsonObject中获取指定键的双精度浮点数值，如果不存在、值为null或无法转换为双精度浮点数，则返回默认值。
 *
 * @param key 需要获取值的键
 * @param defaultValue 当指定键不存在、其对应的值为null或无法转换为双精度浮点数时，返回的默认值
 * @return 指定键所对应的双精度浮点数值，若不存在、值为null或无法转换为双精度浮点数则返回默认值
 */
fun JsonObject.optDouble(key: String, defaultValue: Double = 0.0): Double {
    return this.optDoubleOrNull(key) ?: defaultValue
}

/**
 * 从JsonObject中尝试获取指定键的双精度浮点数值，如果不存在、值为null或无法转换为双精度浮点数，则返回null。
 *
 * @param key 需要获取值的键
 * @return 指定键所对应的双精度浮点数值，若不存在、值为null或无法转换为双精度浮点数则返回null
 */
fun JsonObject.optDoubleOrNull(key: String): Double? {
    return this[key]?.jsonPrimitive?.doubleOrNull
}

/**
 * 从JsonObject中获取指定键的布尔值，如果不存在、值为null或无法转换为布尔值，则返回默认值。
 *
 * @param key 需要获取值的键
 * @param defaultValue 当指定键不存在、其对应的值为null或无法转换为布尔值时，返回的默认值
 * @return 指定键所对应的布尔值，若不存在、值为null或无法转换为布尔值则返回默认值
 */
fun JsonObject.optBoolean(key: String, defaultValue: Boolean = false): Boolean {
    return this.optBooleanOrNull(key) ?: defaultValue
}


/**
 * 从JsonObject中尝试获取指定键的布尔值，如果不存在、值为null或无法转换为布尔值，则返回null。
 *
 * @param key 需要获取值的键
 * @return 指定键所对应的布尔值，若不存在、值为null或无法转换为布尔值则返回null
 */
fun JsonObject.optBooleanOrNull(key: String): Boolean? {
    return this[key]?.jsonPrimitive?.booleanOrNull
}

/**
 * 检查JsonObject中是否存在指定键的值。
 *
 * @param key 需要检查的键
 * @return 如果键存在于JsonObject中且值不为null，则返回true，否则返回false
 */
fun JsonObject.has(key: String): Boolean {
    return this[key] != null
}