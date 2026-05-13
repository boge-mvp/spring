package com.boge

import com.JSON
import com.boge.entity.RedisLock
import com.boge.entity.RedisYaml
import com.isNotNull
import com.logger
import kotlinx.serialization.encodeToString
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * RedisMapper类用于操作Redis数据。
 * 该类依赖于StringRedisTemplate进行Redis的基础操作，并通过RedisYaml配置来管理Redis的连接及其他设置。
 *
 * @param redis StringRedisTemplate对象，提供了一套基于字符串操作的Redis模板方法。
 */
class RedisMapper(val redis: StringRedisTemplate) {

    /**
     * 向 Redis 中添加（或更新）一个键值对数据，其中键为 String 类型，值可以是任意类型 T。
     * 使用 JSON 进行序列化存储，并可选设置过期时间。
     *
     * @param key 存储在 Redis 中的键名，要求为 String 类型。
     * @param value 要存储的数据值，其类型由泛型 T 指定，支持任意可被 JSON 序列化的类型。
     * @param time 可选参数，指定键值对在 Redis 中的存活时长，默认为空。若传入 Duration 实例，则会为键设置过期时间。
     */
    inline fun <reified T> add(key: String, value: T, time: Duration? = null) {
        redis.opsForValue().apply {
            time.isNotNull {
                set(key, JSON.encodeToString(value), this)
            } ?: set(key, JSON.encodeToString(value))
        }
    }

    /**
     * 从 Redis 中获取与给定键关联的值，并将其反序列化为指定泛型 T 的对象。
     *
     * @param key 要从 Redis 中检索的键名，要求为 String 类型。
     * @return 返回键对应的反序列化后的值，如果键不存在或者值无法反序列化为 T 类型，则返回 null。
     */
    inline fun <reified T> get(key: String): T? {
        return redis.opsForValue().get(key)?.let { JSON.decodeFromString(it) }
    }

    /**
     * 从 Redis 中删除指定键及其关联的值。
     *
     * @param key 要从 Redis 中删除的键名，要求为 String 类型。
     * @return 如果删除成功（即键存在并已成功删除），则返回 true；否则返回 false。
     */
    fun remove(key: String): Boolean {
        return redis.delete(key)
    }

    // --------------------- List ---------------------

    /**
     * 为指定键（key）的列表添加一个元素（value）。如果提供了时间（time），则设置该键的过期时间。
     * 此方法通过 JSON 编码将给定的值转换为字符串并将其推送到列表的右侧。
     *
     * @param key 要操作的 Redis 列表键
     * @param value 要添加到列表的元素
     * @param time 可选的过期时间，单位为 Duration 类型，默认为不设置过期时间
     */
    inline fun <reified T> addList(key: String, value: T, time: Duration? = null) {
        redis.boundListOps(key).apply {
            rightPush(JSON.encodeToString(value))
            time.isNotNull { expire(this) }
        }
    }

    /**
     * 向指定键（key）的列表中批量添加一组元素（value）。如果提供了时间（time），则设置该键的过期时间。
     * 此方法遍历输入列表，对每个元素进行 JSON 编码后，将编码后的字符串数组整体推送到列表的右侧。
     *
     * @param key 要操作的 Redis 列表键
     * @param value 要批量添加到列表的一组元素
     * @param time 可选的过期时间，单位为 Duration 类型，默认为不设置过期时间
     */
    inline fun <reified T> addList(key: String, value: List<T>, time: Duration? = null) {
        redis.boundListOps(key).apply {
            rightPushAll(*value.map { JSON.encodeToString(it) }.toTypedArray())
            time.isNotNull { expire(this) }
        }
    }

    /**
     * 获取指定键（key）的整个 Redis 列表，并将其元素解码为指定类型 T 的列表。
     * 如果列表不存在，则返回空列表。
     *
     * @param key 要获取的 Redis 列表键
     * @return 解码后的类型为 T 的列表
     */
    inline fun <reified T> getList(key: String): List<T> {
        return getList(key, 0, -1)
    }

    /**
     * 获取指定键（key）的 Redis 列表中从 start 索引到 end 索引的子列表，并将其元素解码为指定类型 T 的列表。
     * 如果索引范围无效或列表不存在，则返回空列表。
     *
     * @param key 要获取的 Redis 列表键
     * @param start 开始索引（包含）
     * @param end 结束索引（包含）
     * @return 解码后的类型为 T 的子列表
     */
    inline fun <reified T> getList(key: String, start: Long, end: Long): List<T> {
        return redis.opsForList().range(key, start, end)?.map { JSON.decodeFromString(it) } ?: emptyList()
    }

    /**
     * 获取指定键（key）的 Redis 列表的长度（元素个数）。
     *
     * @param key 要查询长度的 Redis 列表键
     * @return 列表中的元素个数，若列表不存在则返回 0
     */
    fun getListSize(key: String): Long {
        return redis.opsForList().size(key) ?: 0
    }

    /**
     * 根据指定索引（index）从指定键（key）的 Redis 列表中获取一个元素，并将其解码为指定类型 T。
     * 如果索引超出范围或者列表为空，则返回 null。
     *
     * @param key 要获取元素的 Redis 列表键
     * @param index 目标元素所在的位置索引
     * @return 解码后的类型为 T 的元素，若索引无效则返回 null
     */
    inline fun <reified T> getList(key: String, index: Long): T? {
        return redis.opsForList().index(key, index)?.let { JSON.decodeFromString(it) }
    }

    /**
     * 更新指定键（key）的 Redis 列表中指定索引（index）处的元素为新值（value）。
     * 新值会先通过 JSON 编码转换为字符串再更新到列表中。
     *
     * @param key 要更新元素的 Redis 列表键
     * @param index 要更新的元素位置索引
     * @param value 新的元素值
     */
    inline fun <reified T> updateListIndex(key: String, index: Long, value: T) {
        redis.opsForList()[key, index] = JSON.encodeToString(value)
    }

    /**
     * 从指定键（key）的 Redis 列表中移除与给定值（value）相匹配的元素。
     * 根据 count 参数决定移除的元素个数，count 为 0 表示移除所有匹配项。
     *
     * @param key 要操作的 Redis 列表键
     * @param value 要移除的元素值
     * @param count 可选的移除次数，默认为 0，表示移除全部匹配项
     * ```
     * count > 0：从列表的头部开始向尾部搜索，删除与给定值相等的元素，直到删除数量达到count为止。
     * count < 0：从列表的尾部开始向头部搜索，删除与给定值相等的元素，直到删除数量达到count的绝对值为止。
     * count = 0：删除列表中所有与给定值相等的元素。
     * ```
     * @return 实际移除的元素个数，若没有找到匹配项则返回 0
     */
    inline fun <reified T> removeList(key: String, value: T, count: Long = 0): Long {
        return redis.opsForList().remove(key, count, JSON.encodeToString(value)) ?: 0
    }

    // --------------------- Hash ---------------------

    inline fun <reified K : Any, reified T> getHash(key: String, item: K): T? {
        return redis.opsForHash<K, String>()[key, item]?.let { JSON.decodeFromString(it) }
    }

    inline fun <reified K : Any, reified T> getHash(key: String, item: Collection<K>): List<T?> {
        return redis.opsForHash<K, String>()
            .multiGet(key, item)
            .map {
                if (it != null) JSON.decodeFromString<T>(it) else null
            }
    }

    inline fun <reified K, reified T> getHashMap(key: String): Map<K, T> {
        return redis.opsForHash<K, String>().entries(key).mapValues { JSON.decodeFromString(it.value) }
    }

    inline fun <reified K : Any> getHashKey(key: String): MutableSet<K> {
        return redis.opsForHash<K, String>().keys(key)
    }

    inline fun <reified K : Any> getHashSize(key: String): Long {
        return redis.opsForHash<K, String>().size(key)
    }

    inline fun <reified K : Any> getHashScan(
        key: String,
        options: ScanOptions
    ): Cursor<MutableMap.MutableEntry<K, String>> {
        return redis.opsForHash<K, String>().scan(key, options)
    }

    inline fun <reified K : Any, reified T> getHashValue(key: String): List<T> {
        return redis.opsForHash<K, String>().values(key).map { JSON.decodeFromString<T>(it) }
    }


    inline fun <reified K, reified T> addHash(key: String, map: Map<K, T>, time: Duration? = null) {
        redis.boundHashOps<K, String>(key).apply {
            val value = map.mapValues { JSON.encodeToString(it.value) }
            putAll(value)
            time.isNotNull { expire(this) }
        }
    }

    inline fun <reified K : Any, reified T : Any> addHash(key: String, item: K, value: T, time: Duration? = null) {
        redis.boundHashOps<K, String>(key).apply {
            put(item, JSON.encodeToString(value))
            time.isNotNull { expire(this) }
        }
    }

    inline fun <reified K, reified T> removeHash(key: String, vararg item: T) {
        redis.opsForHash<K, String>().delete(key, *item.map { JSON.encodeToString(it) }.toTypedArray())
    }

    inline fun <reified K : Any> hasHash(key: String, item: K): Boolean {
        return redis.opsForHash<K, String>().hasKey(key, item)
    }

    inline fun <reified K : Any, reified T> hincr(key: String, item: K, delta: Double): Double {
        return redis.opsForHash<K, T>().increment(key, item, delta)
    }

    inline fun <reified K : Any, reified T> hdecr(key: String, item: K, delta: Double): Double {
        return redis.opsForHash<K, T>().increment(key, item, -delta)
    }

    // --------------------- Set ---------------------

    inline fun <reified T> getSet(key: String): List<T> {
        return (redis.opsForSet().members(key)?.map { JSON.decodeFromString(it) }) ?: emptyList()
    }

    inline fun <reified T> hasSetKey(key: String, value: T): Boolean {
        return redis.opsForSet().isMember(key, JSON.encodeToString(value)) == true
    }

    inline fun <reified T> addSet(key: String, value: T, time: Duration? = null): Long {
        return redis.boundSetOps(key).run {
            time.isNotNull { expire(this) }
            add(JSON.encodeToString(value))
        } ?: 0
    }

    inline fun <reified T> addSet(key: String, value: Collection<T>, time: Duration? = null): Long {
        return redis.boundSetOps(key).run {
            time.isNotNull { expire(this) }
            add(*value.map { JSON.encodeToString(it) }.toTypedArray())
        } ?: 0
    }

    fun getSetSize(key: String): Long {
        return redis.opsForSet().size(key) ?: 0
    }

    inline fun <reified T> removeSet(key: String, vararg values: T): Long? {
        return redis.opsForSet().remove(key, *values.map { JSON.encodeToString(it) }.toTypedArray())
    }

    /**
     * 使用互斥锁，防止在并发下发生缓存击穿
     * ```
     * val name = "lockTest"
     * val sleep = 5L
     * (1..10).forEach {
     *     thread {
     *         val string: String? = redisMapper.lock(
     *             name,
     *             RedisLock(
     *                 cacheTime = Duration.ofSeconds(30),
     *                 maxWaitTime = Duration.ofSeconds(it.toLong()),
     *                 read = {
     *                     redisMapper.getList(name, 0)
     *                 },
     *                 readDatabase = {
     *                     logger.info("读数据")
     *                     TimeUnit.SECONDS.sleep(sleep)
     *                     "ccc"
     *                 },
     *                 write = {
     *                     logger.info("保存了数据")
     *                     redisMapper.addList(name, "ccc", it.cacheTime)
     *                 },
     *             )
     *         )
     *         logger.info("结果:$string")
     *
     *     }
     * }
     * TimeUnit.SECONDS.sleep(sleep + 10)
     * ```
     */
    inline fun <reified T : Any> lock(key: String, lockTime: RedisLock<T> = RedisLock()): T? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < lockTime.maxWaitTime.toMillis()) {
            // 尝试获取缓存
            val cachedUser = lockTime.read?.invoke(lockTime) ?: get<T>(key)
            if (cachedUser != null) {
                return cachedUser
            }
            // 使用分布式锁
            val lockKey = "lock:$key"
            if (redis.opsForValue().setIfAbsent(lockKey, "1", lockTime.lockTime) == true) {  // 设置一个有超时时间的锁
                val result = runCatching {
                    // 再次检查缓存（避免在获取锁的过程中其他线程已填充缓存）
                    val doubleCheckCached = lockTime.read?.invoke(lockTime) ?: get<T>(key)
                    if (doubleCheckCached != null) {
                        return doubleCheckCached
                    }
                    // 缓存未命中，从数据库加载数据
                    val database = lockTime.readDatabase?.invoke()
                    // 存入缓存
                    lockTime.write?.invoke(lockTime) ?: add(key, database, lockTime.cacheTime)
                    database
                }
                // 无论如何，解锁
                redis.delete(lockKey)
                return result.getOrNull()
            } else {
                // 如果未能获取到锁，则等待一段时间后重试
                TimeUnit.MILLISECONDS.sleep(50)
                // 尝试再次获取
            }
        }
        return null
    }

    /**
     * 锁定执行块
     * @param key 锁名字
     * @param block 执行方法
     * @param maxWaitTime 最大等待时长 超时就直接调用block,不再等待
     * @param lockTime 保持线程安全的锁 最大锁定时长 默认5m
     * @param sleep 重试等待时长
     */
    fun lockBlock(
        key: String,
        block: () -> Unit,
        maxWaitTime: Duration = Duration.ofSeconds(60),
        lockTime: Duration = Duration.ofMinutes(5),
        sleep: Duration = Duration.ofMillis(50)
    ) {
        // 获取当前时间
        val startTime = System.nanoTime()
        // 使用分布式锁

        val lockKey = "lockBlock:$key"
        // 标记是否执行了需要的代码块
        var isRun = false
        // 循环尝试获取锁
        while (System.nanoTime() - startTime < maxWaitTime.toNanos()) {
            // 尝试设置一个有超时时间的锁
            if (redis.opsForValue().setIfAbsent(lockKey, "1", lockTime) == true) {
                // 标记为执行了代码块
                isRun = true
                try {
                    // 执行代码块
                    block()
                } catch (e: Exception) {
                    // 处理异常，例如记录日志
                    logger.error("执行${key}代码块时发生异常", e)
                } finally {
                    // 无论如何，解锁
                    redis.delete(lockKey)
                }
                // 退出循环
                break
            } else {
                // 如果未能获取到锁，则等待一段时间后重试
                TimeUnit.NANOSECONDS.sleep(sleep.toNanos())
                // 尝试再次获取锁
            }
        }
        // 超时还未执行
        if (!isRun) {
            // 记录警告信息
            logger.warn("lockBlock: ${key}在${TimeUnit.NANOSECONDS.toMillis(startTime)}开始等待至${System.currentTimeMillis()}, 在${maxWaitTime.toMillis()}ms后超时，现立即执行")
            // 直接执行代码块
            block()
        }
    }


}