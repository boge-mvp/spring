package com.boge.redis

import com.boge.utils.BeanUtils
import com.isNotNull
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.convert.CustomConversions
import org.springframework.data.geo.Point
import org.springframework.data.redis.connection.DataType
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.convert.*
import org.springframework.data.redis.core.mapping.RedisMappingContext
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener
import org.springframework.data.redis.repository.query.RedisOperationChain.PathAndValue
import org.springframework.data.redis.util.ByteUtils
import org.springframework.lang.Nullable
import org.springframework.util.Assert
import org.springframework.util.CollectionUtils
import org.springframework.util.ObjectUtils
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicReference

class RedisKeyValueAdapter<K, V>(
    private val redisTemplate: RedisTemplate<K, V>,
    mappingContext: RedisMappingContext,
    customConversions: CustomConversions
) : org.springframework.data.redis.core.RedisKeyValueAdapter(redisTemplate, mappingContext, customConversions) {

    private val enableKeyspaceEvents: EnableKeyspaceEvents
    private lateinit var localExpirationListener: AtomicReference<KeyExpirationEventMessageListener>
    private val initKeyExpirationListener: Method
    private val expires: Method
    private val keepShadowCopy: Method
    private val PHANTOM_KEY_TTL: Int

    init {

        setEnableKeyspaceEvents(EnableKeyspaceEvents.ON_STARTUP)
        val cls = org.springframework.data.redis.core.RedisKeyValueAdapter::class.java
        this.PHANTOM_KEY_TTL = BeanUtils.getField(cls, "PHANTOM_KEY_TTL", true)!!.get(this) as Int
        this.enableKeyspaceEvents =
            BeanUtils.getField(cls, "enableKeyspaceEvents", true)!!.get(this) as EnableKeyspaceEvents

        this.initKeyExpirationListener = BeanUtils.getMethod(cls, "initKeyExpirationListener", true)!!
        this.expires = BeanUtils.getMethod(cls, "expires", true, RedisData::class.java)!!
        this.keepShadowCopy = BeanUtils.getMethod(cls, "keepShadowCopy", true)!!

    }

    override fun put(id: Any, item: Any, keyspace: String): Any {

        @Suppress("UNCHECKED_CAST")
        if (!::localExpirationListener.isInitialized) this.localExpirationListener = BeanUtils.getField(
            org.springframework.data.redis.core.RedisKeyValueAdapter::class.java,
            "expirationListener",
            true
        )!!.get(this) as AtomicReference<KeyExpirationEventMessageListener>

        val rdo = if (item is RedisData) item else RedisData()

        if (item !is RedisData) {
            converter.write(item, rdo)
        }

        if (ObjectUtils.nullSafeEquals(EnableKeyspaceEvents.ON_DEMAND, enableKeyspaceEvents)
            && localExpirationListener.get() == null
        ) {
            if (rdo.timeToLive != null && rdo.timeToLive!! > 0) {
                initKeyExpirationListener.invoke(this)
            }
        }

        if (rdo.id == null) {
            rdo.id = converter.conversionService.convert(id, String::class.java)
        }
        redisTemplate.execute(RedisCallback<Any?> { connection ->
            val key = toBytes(rdo.id!!)
            val objectKey = createKey(rdo.keyspace!!, rdo.id!!)

            val isNew = connection.keyCommands().del(*arrayOf(objectKey)) == 0L

            connection.hashCommands().hMSet(objectKey, rdo.bucket.rawMap())

            if (isNew) {
                connection.setCommands().sAdd(
                    toBytes(rdo.keyspace!!),
                    *arrayOf(key)
                )
            }

            if (expires.invoke(this, rdo) as Boolean) {
                connection.keyCommands().expire(objectKey, rdo.timeToLive!!)
            }

            if (keepShadowCopy.invoke(this) as Boolean) { // add phantom key so values can be restored
                val phantomKey =
                    ByteUtils.concat(objectKey, MappingRedisConverter.BinaryKeyspaceIdentifier.PHANTOM_SUFFIX)
                if (expires.invoke(this, rdo) as Boolean) {
                    connection.keyCommands().del(*arrayOf(phantomKey))
                    connection.hashCommands().hMSet(phantomKey, rdo.bucket.rawMap())
                    connection.keyCommands().expire(
                        phantomKey,
                        rdo.timeToLive!! + PHANTOM_KEY_TTL
                    )
                } else if (!isNew) {
                    connection.keyCommands().del(*arrayOf(phantomKey))
                }
            }

            val indexWriter = IndexWriter(connection, converter)
            if (isNew) {
                indexWriter.createIndexes(key, rdo.indexedData, rdo.timeToLive)
            } else {
                indexWriter.deleteAndUpdateIndexes(key, rdo.indexedData, rdo.timeToLive)
            }
            null
        })
        return item
//        return super.put(id, item, keyspace)
    }

}

data class KeySelector(val keys: Collection<ByteArray>, val setValueLookup: Set<PathAndValue>) {
    companion object {
        fun of(converter: RedisConverter, pathAndValues: Set<PathAndValue>, domainType: Class<*>?): KeySelector {
            val keys: MutableSet<ByteArray> = LinkedHashSet()
            val remainder: MutableSet<PathAndValue> = LinkedHashSet()

            for (pathAndValue in pathAndValues) {
                val path = converter.mappingContext
                    .getPersistentPropertyPath(pathAndValue.path, domainType!!)
                if (path.leafProperty.isIdProperty) {
                    val key = converter.conversionService.convert(
                        pathAndValue.firstValue,
                        ByteArray::class.java
                    )
                    keys.add(key!!)
                } else {
                    remainder.add(pathAndValue)
                }
            }

            return KeySelector(keys, remainder)
        }
    }
}

/**
 * Creates new [IndexWriter].
 *
 * @param connection must not be null.
 * @param converter must not be null.
 */
internal class IndexWriter(
    connection: RedisConnection,
    converter: RedisConverter
) {
    private val connection: RedisConnection
    private val converter: RedisConverter

    init {
        Assert.notNull(connection, "RedisConnection cannot be null")
        Assert.notNull(converter, "RedisConverter cannot be null")

        this.connection = connection
        this.converter = converter
    }

    /**
     * Initially creates indexes.
     *
     * @param key must not be null.
     * @param indexValues can be null.
     */
    fun createIndexes(key: Any, indexValues: Iterable<IndexedData>?, timeToLive: Long?) {
        createOrUpdateIndexes(key, indexValues, IndexWriteMode.CREATE, timeToLive)
    }

    /**
     * Updates indexes by first removing key from existing one and then persisting new index data.
     *
     * @param key must not be null.
     * @param indexValues can be null.
     */
    fun updateIndexes(key: Any, indexValues: Iterable<IndexedData>?, timeToLive: Long?) {
        createOrUpdateIndexes(key, indexValues, IndexWriteMode.PARTIAL_UPDATE, timeToLive)
    }

    /**
     * Updates indexes by first removing key from existing one and then persisting new index data.
     *
     * @param key must not be null.
     * @param indexValues can be null.
     */
    fun deleteAndUpdateIndexes(key: Any, @Nullable indexValues: Iterable<IndexedData>?, timeToLive: Long?) {
        createOrUpdateIndexes(key, indexValues, IndexWriteMode.UPDATE, timeToLive)
    }

    private fun createOrUpdateIndexes(
        key: Any, @Nullable indexValues: Iterable<IndexedData>?,
        writeMode: IndexWriteMode,
        timeToLive: Long?
    ) {
        Assert.notNull(key, "Key must not be null")
        if (indexValues == null) {
            return
        }

        val binKey = toBytes(key)

        if (ObjectUtils.nullSafeEquals(IndexWriteMode.UPDATE, writeMode)) {
            if (indexValues.iterator().hasNext()) {
                val data = indexValues.iterator().next()
                removeKeyFromIndexes(data.keyspace, binKey)
            }
        } else if (ObjectUtils.nullSafeEquals(IndexWriteMode.PARTIAL_UPDATE, writeMode)) {
            removeKeyFromExistingIndexes(binKey, indexValues)
        }

        addKeyToIndexes(binKey, indexValues, timeToLive)
    }

    /**
     * Removes a key from all available indexes.
     *
     * @param key must not be null.
     */
    fun removeKeyFromIndexes(keyspace: String, key: Any?) {
        Assert.notNull(key, "Key must not be null")

        val binKey = toBytes(key)
        val indexHelperKey = ByteUtils.concatAll(toBytes("$keyspace:"), binKey, toBytes(":idx"))

        for (indexKey in connection.setCommands().sMembers(indexHelperKey)!!) {
            val type = connection.keyCommands().type(indexKey!!)
            if (DataType.ZSET == type) {
                connection.zSetCommands().zRem(indexKey, *arrayOf(binKey))
            } else {
                connection.setCommands().sRem(indexKey, *arrayOf(binKey))
            }
        }

        connection.keyCommands().del(*arrayOf(indexHelperKey))
    }

    /**
     * Removes all indexes.
     */
    fun removeAllIndexes(keyspace: String) {
        val potentialIndex = connection.keyCommands().keys(toBytes("$keyspace:*")!!)!!

        if (potentialIndex.isNotEmpty()) {
            connection.keyCommands().del(*potentialIndex.toTypedArray<ByteArray>())
        }
    }

    private fun removeKeyFromExistingIndexes(key: ByteArray?, indexValues: Iterable<IndexedData>) {
        for (indexData: IndexedData in indexValues) {
            removeKeyFromExistingIndexes(key, indexData)
        }
    }

    /**
     * Remove given key from all indexes matching [IndexedData.getIndexName]:
     *
     * @param key
     * @param indexedData
     */
    protected fun removeKeyFromExistingIndexes(key: ByteArray?, indexedData: IndexedData) {
        Assert.notNull(indexedData, "IndexedData must not be null")

        val existingKeys = connection.keyCommands()
            .keys(toBytes(indexedData.keyspace + ":" + indexedData.indexName + ":*")!!)!!

        if (!CollectionUtils.isEmpty(existingKeys)) {
            for (existingKey: ByteArray? in existingKeys) {
                if (indexedData is GeoIndexedPropertyValue) {
                    connection.geoCommands().geoRemove(existingKey!!, *arrayOf(key))
                } else {
                    connection.setCommands().sRem(existingKey!!, *arrayOf(key))
                }
            }
        }
    }

    private fun addKeyToIndexes(key: ByteArray?, indexValues: Iterable<IndexedData>, timeToLive: Long?) {
        for (indexData: IndexedData in indexValues) {
            addKeyToIndex(key, indexData, timeToLive)
        }
    }

    /**
     * Adds a given key to the index for [IndexedData.getIndexName].
     *
     * @param key must not be null.
     * @param indexedData must not be null.
     */
    protected fun addKeyToIndex(key: ByteArray?, indexedData: IndexedData, timeToLive: Long?) {
        Assert.notNull(key, "Key must not be null")
        Assert.notNull(indexedData, "IndexedData must not be null")

        if (indexedData is RemoveIndexedData) {
            return
        }

        if (indexedData is SimpleIndexedPropertyValue) {

            val value = indexedData.value ?: return

            if (value == null) return

            var indexKey = toBytes(indexedData.getKeyspace() + ":" + indexedData.getIndexName() + ":")
            indexKey = ByteUtils.concat(indexKey!!, toBytes(value)!!)
            connection.setCommands().sAdd(indexKey, *arrayOf(key))
            timeToLive.isNotNull {
                connection.keyCommands().expire(indexKey, this)
            }

            // keep track of indexes used for the object
            val idxKey = ByteUtils.concatAll(toBytes(indexedData.getKeyspace() + ":"), key, toBytes(":idx"))
            connection.setCommands().sAdd(
                idxKey,
                *arrayOf(indexKey)
            )
            timeToLive.isNotNull {
                connection.keyCommands().expire(idxKey, this)
            }

        } else if (indexedData is GeoIndexedPropertyValue) {
            val value: Point = indexedData.value ?: return

            val indexKey = toBytes(indexedData.getKeyspace() + ":" + indexedData.getIndexName())
            connection.geoCommands().geoAdd(indexKey!!, indexedData.point, key!!)
            timeToLive.isNotNull {
                connection.keyCommands().expire(indexKey, this)
            }

            // keep track of indexes used for the object
            val idxKey = ByteUtils.concatAll(toBytes(indexedData.getKeyspace() + ":"), key, toBytes(":idx"))
            connection.setCommands().sAdd(
                idxKey,
                *arrayOf(indexKey)
            )
            timeToLive.isNotNull {
                connection.keyCommands().expire(idxKey, this)
            }
        } else {
            throw IllegalArgumentException(
                String.format("Cannot write index data for unknown index type %s", indexedData.javaClass)
            )
        }
    }

    private fun toBytes(@Nullable source: Any?): ByteArray? {
        if (source == null) {
            return byteArrayOf()
        }

        if (source is ByteArray) {
            return source
        }

        if (converter.conversionService.canConvert(source.javaClass, ByteArray::class.java)) {
            return converter.conversionService.convert(source, ByteArray::class.java)
        }

        throw InvalidDataAccessApiUsageException(
            String.format(
                "Cannot convert %s to binary representation for index key generation; "
                        + "Are you missing a Converter; Did you register a non PathBasedRedisIndexDefinition that might apply to a complex type",
                source.javaClass
            )
        )
    }

    /**
     * @since 1.8
     */
    private enum class IndexWriteMode {
        CREATE, UPDATE, PARTIAL_UPDATE
    }
}