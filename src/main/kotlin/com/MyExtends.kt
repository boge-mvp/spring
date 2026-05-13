package com

import com.boge.annotation.FilterIgnore
import com.boge.serialization.TimestampSerializer
import icu.mhb.mybatisplus.plugln.base.mapper.JoinBaseMapper
import icu.mhb.mybatisplus.plugln.base.service.impl.JoinServiceImpl
import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.checkerframework.common.returnsreceiver.qual.This
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.framework.AopContext
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * 判断自己内部的属性全是空值
 */
inline fun <reified T : Any> T.isAllPropertiesNull(): Boolean {
    val properties = T::class.declaredMemberProperties
    for (property in properties) {
        property.isAccessible = true
        val value = property.get(this)
        property.isAccessible = false
        if (value != null) {
            return false
        }
    }
    return true
}

inline val <reified T> T.logger: Logger
    get() = LoggerFactory.getLogger(T::class.java)
inline val <reified T> T.log: Logger
    get() = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> logName(string: String): Logger {
    return LoggerFactory.getLogger(string)
}

/**
 * 将当前数组的元素按照指定长度进行组合
 * @param positions 组合长度
 * @param distinct 排除重复数据 包括本身
 * @sample
 * [0,1,2]
 * distinct=false: [[2, 2], [2, 1], [2, 0], [1, 2], [1, 1], [1, 0], [0, 2], [0, 1], [0, 0]]
 * distinct=true: [[1, 2], [0, 2], [0, 1]]
 */
inline fun <reified T> List<T>.combinations(
    positions: Int,
    distinct: Boolean = true
): MutableList<List<T>> {
    // 最终要返回的
    val combinations = mutableListOf<List<T>>()
    val numbers = size
    val stack = mutableListOf<Triple<List<T>, Int, Int>>()
    forEachIndexed { index, value ->
        stack.add(Triple(mutableListOf(value), positions - 1, if (distinct) index else 0))
    }
    while (stack.isNotEmpty()) {
        val (currentCombination, remainingPositions, startNumber) = stack.removeLast()
        if (remainingPositions == 0) {
            combinations.add(currentCombination)
        } else {
            val start = startNumber + if (distinct) 1 else 0
            for (i in start..<numbers) {
                val newCombination = currentCombination + get(i)
                stack.add(Triple(newCombination, remainingPositions - 1, if (distinct) i else 0))
            }
        }
    }
    return combinations
}


@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Slf4j {

//    companion object {
//
//        val <reified T> T.log: Logger
//            inline get() = LoggerFactory.getLogger(T::class.java)
//
//    }

}

@OptIn(ExperimentalSerializationApi::class)
val JSON
    get() = Json {
        // 指定是否编码默认值。默认情况下，如果属性的值与其默认值相同，将不会包含在生成的JSON中。通过设置encodeDefaults为true，可以强制包含所有属性，无论其是否与默认值相同。
        encodeDefaults = false
        // 指定是否忽略未知的JSON字段。默认情况下，如果JSON中包含未知的字段，将会抛出异常。通过设置ignoreUnknownKeys为true，可以忽略未知的字段而不抛出异常。
        ignoreUnknownKeys = true
        // 指定是否宽松解析JSON。默认情况下，JSON解析器对于不符合严格JSON规范的输入会抛出异常。通过设置isLenient为true，可以允许一些非严格的JSON语法，如单引号括起的字符串、未引号包围的键等。
        isLenient = true
        // 指定是否允许使用复杂的键类型。默认情况下，JSON解析器只接受字符串作为键。通过设置allowStructuredMapKeys为true，可以允许使用复杂的键类型，如自定义对象作为键。
        allowStructuredMapKeys = false
        // 指定是否对生成的JSON进行格式化美化。默认情况下，生成的JSON是紧凑的，没有额外的空格和换行符。通过设置prettyPrint为true，可以生成格式化良好的JSON，便于阅读和调试。
        prettyPrint = false
        // 允许解析器接受 JSON 对象和数组中的尾随（结束）逗号，使输入类似 [1, 2, 3,] 有效。 不影响编码。 false 默认情况下。
        allowTrailingComma = true

        serializersModule = SerializersModule {
            contextual(Timestamp::class, TimestampSerializer)
        }
    }

/**
 * 压缩
 */
fun ByteArray.deflate(): ByteArray {
    return Deflater().also { it.setInput(this); it.finish() }
        .let { deflate ->
            ByteArrayOutputStream(this.size).use {
                ByteArray(1024).let { buffer ->
                    while (!deflate.finished()) {
                        val count = deflate.deflate(buffer)
                        it.write(buffer, 0, count)
                    }
                    deflate.end()
                }
                it
            }.toByteArray()
        }
}

/**
 * 解压
 */
fun ByteArray.inflate(): ByteArray {
    return Inflater().also { it.setInput(this) }
        .let { inflate ->
            ByteArray(1024).let { buffer ->
                ByteArrayOutputStream(this.size).use {
                    while (!inflate.finished()) {
                        val count = inflate.inflate(buffer)
                        it.write(buffer, 0, count)
                    }
                    inflate.end()
                    it
                }.toByteArray()
            }
        }
}

open class AttachBox(var value: Any? = null)

/**
 * 扩展一个附加属性值
 */
val <T : Any> Observable<T>.attach: AttachBox by lazy { AttachBox() }


/**
 *
 * 判断类型满足条件 则执行`block`函数,并将 `this` 作为value接收器，并返回其结果
 * ```
 * Boolean true
 * CharSequence isNotEmpty
 * Int/Long >0
 * List/Array/ByteArray isNotEmpty
 * ```
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R> T.ifOk(block: T.() -> R): R {
    return when {
        this is Boolean && this -> block()
        this is CharSequence && this.isNotEmpty() -> block()
        this is Int && this > 0 -> block()
        this is Long && this > 0 -> block()
        this is Collection<*> && this.isNotEmpty() -> block()
        this is Array<*> && this.isNotEmpty() -> block()
        this is Map<*, *> && this.isNotEmpty() -> block()
        this is ByteArray && this.isNotEmpty() -> block()
        else -> this as R
    }
}

/**
 * `this` 作为value接收器，并返回其结果
 */
@Suppress("UNCHECKED_CAST")
inline fun <T, R> T?.isNull(block: T?.() -> R): R {
    if (this == null) {
        return block()
    }
    return this as R
}

/**
 * `this` 作为value接收器，并返回其结果
 */
inline fun <T, R> T?.isNotNull(block: T.() -> R?): R? {
    return when {
        this != null -> block()
        else -> null
    }
}

/**
 * 判断是否是true
 *
 * 是true执行 block并返回结果 不是则直接返回null
 */
inline fun <T> Boolean.ok(block: () -> T): T? {
    if (this)
        return block()
    return null
}

inline fun Boolean.okSelf(block: () -> Unit): Boolean {
    if (this) block()
    return this
}

/**
 * 获取当前代理的对象
 */
inline fun <reified T : JoinServiceImpl<M, V>, M : JoinBaseMapper<V>, V> T.thatProxy(): T {
    return T::class.java.cast(AopContext.currentProxy()) as T
}

/**
 * 为给定的对象合并属性值。此函数会比较两个对象的属性值，并将不同值的属性从第二个对象复制到第一个对象的新实例中。
 * 如果[isApply]为true，则会将更改应用到原始的第一个对象上。
 * 如果[isIgnoreNull]为true，则会忽略值为null的属性。
 *
 * @param T 被合并属性的对象类型，必须是Any的子类型。
 * @param other 第二个对象，用于比较和合并属性。
 * @param isApply 是否将合并的属性值应用到原始的第一个对象上，默认为false。
 * @param isIgnoreNull 是否忽略值为null的属性，默认为true。
 * @return 返回一个新的对象实例，其中包含了合并后的属性值。如果没有任何属性被合并，则返回null。
 */
inline fun <reified T : Any> T.unionProp(other: T, isApply: Boolean = false, isIgnoreNull: Boolean = true): T? {
    // 筛选出两个对象中不同且没有被@FilterIgnore注解标记的属性
    val props = this::class.memberProperties.filter { prop ->
        (prop.getter.call(this) != prop.getter.call(other)) && !prop.hasAnnotation<FilterIgnore>()
    }
    // 如果存在不同的属性，则创建新对象并复制属性值
    if (props.isNotEmpty()) {
        var newClass: T? = null
        props.forEach { prop ->
            val propValue = prop.getter.call(other)
            // 如果指定不忽略null值或当前属性值不为null，则进行属性赋值
            if (!isIgnoreNull || propValue != null) {
                if (prop is KMutableProperty<*>) {
                    if (newClass == null) newClass = T::class.createInstance()
                    // 给新对象赋值
                    prop.setter.call(newClass, propValue)
                    // 如果指定应用更改，则给原始对象赋值
                    if (isApply)
                        prop.setter.call(this, propValue)
                }
            }
        }
        return newClass
    }
    // 如果没有需要合并的属性，则返回null
    return null
}

/**
 * 将对象[other]的属性值应用到当前对象上。该函数首先筛选出两个对象之间值不相同且没有被[FilterIgnore]注解标记的属性，
 * 然后将[other]对象的属性值赋给当前对象。
 *
 * @param T 被应用属性值的对象类型，必须是Any的子类型。
 * @param other 提供属性值的对象。
 * @param isIgnoreNull 是否忽略为空的属性值。如果为true，则当属性值为空时，不会将该属性值应用到当前对象上，默认为true。
 * @return 返回当前对象，以便于链式调用。
 */
inline fun <reified T : Any> T.applyProp(other: T, isIgnoreNull: Boolean = true): T {
    this.applyPropEd(other, isIgnoreNull)
    // 返回当前对象
    return this
}

/**
 * 为对象应用另一个对象的属性值。此函数主要用于比较两个对象的属性值，并将第一个对象的属性值更新为第二个对象的属性值，
 * 但仅限于那些在两个对象中实际不同且未被[FilterIgnore]注解标记的属性。
 *
 * @param other 要从中获取属性值的对象。该对象类型必须与调用者的类型相同。
 * @param isIgnoreNull 是否忽略值为null的属性。如果为true，则当发现属性值为null时，不会尝试更新该属性。
 * @return 返回一个布尔值，表示是否有属性被成功更新。
 */
inline fun <reified T : Any> T.applyPropEd(other: T, isIgnoreNull: Boolean = true): Boolean {
    // 筛选出当前对象和other对象中值不相同且没有被[FilterIgnore]注解标记的属性
    val props = T::class.memberProperties.filter { prop ->
        (prop.get(this) != prop.get(other)) && !prop.hasAnnotation<FilterIgnore>()
    }
    var result = false // 用于标记是否有属性被成功更新

    // 当存在需要更新的属性时
    if (props.isNotEmpty()) {
        props.forEach { prop ->
            // 获取other对象的属性值
            val propValue = prop.get(other)
            // 如果isIgnoreNull为true且属性值为null，则跳过该属性；否则尝试更新属性值
            if (!isIgnoreNull || propValue != null) {
                // 检查属性是否可变，如果是，则更新属性值，并标记结果为true
                if (prop is KMutableProperty<*>) {
                    result = true
                    prop.setter.call(this, propValue)
                }
            }
        }
    }
    return result // 返回是否有属性被成功更新
}

/**
 * 创建一个包含当前堆栈跟踪的异常实例。
 *
 * @param message 异常的可选消息。
 * @return Throwable实例，其堆栈跟踪被设置为当前线程的堆栈跟踪。
 */
fun currentStackTrace(message: String? = null) = Exception(message)

/**
 * 重试机制函数，用于在操作失败时自动尝试重新操作
 *
 * @param count 重试次数，默认为1次
 * @param callback 包含具体操作的回调函数
 * @param T 泛型T，表示调用该函数的类类型
 * @param R 泛型R，表示回调函数的返回值类型
 *
 * @return 返回回调函数的返回值类型，可能为null
 *
 * 此函数会在操作失败时，自动重试指定次数，并捕获异常
 * 如果重试次数用尽且操作仍然失败，则记录错误日志
 */
inline fun <reified T, R> T.errorRetry(count: Int = 1, callback: T.() -> R?): R? {
    var result: R? = null
    for (i in 0..count) {
        try {
            result = callback()
        } catch (e: Exception) {
            if (count == i) {
                logger.error("errorRetry", e)
            }
        }
    }
    return result
}
