package com.boge.redis

import org.springframework.data.redis.repository.query.RedisOperationChain
import org.springframework.data.redis.repository.query.RedisPartTreeQuery
import org.springframework.data.repository.query.ParameterAccessor
import org.springframework.data.repository.query.parser.Part
import org.springframework.data.repository.query.parser.PartTree

/**
 * @see RedisPartTreeQuery
 * @see org.springframework.data.redis.repository.query.RedisQueryCreator
 */
class RedisQueryCreator(tree: PartTree, parameters: ParameterAccessor) :
    org.springframework.data.redis.repository.query.RedisQueryCreator(tree, parameters) {

    override fun create(part: Part, iterator: MutableIterator<Any>): RedisOperationChain =
        if (part.type == Part.Type.IN) from(part, iterator, RedisOperationChain()) else super.create(part, iterator)

    override fun and(part: Part, base: RedisOperationChain, iterator: MutableIterator<Any>): RedisOperationChain =
        if (part.type == Part.Type.IN) from(part, iterator, base) else super.and(part, base, iterator)

    private fun from(part: Part, iterator: MutableIterator<Any>, sink: RedisOperationChain): RedisOperationChain {
        when (part.type) {
            Part.Type.IN -> {
                val path = part.property.toDotPath()
                while (iterator.hasNext()) {
                    when (val value = iterator.next()) {
                        is Collection<*> -> {
                            if (value.size == 1) {
                                sink.sismember(path, value.first()!!)
                            } else value.forEach { f ->
                                sink.orSismember(path, f!!)
                            }
                        }

                        is IntArray -> {
                            if (value.size == 1) {
                                sink.sismember(path, value.first())
                            } else value.forEach { f ->
                                sink.orSismember(path, f)
                            }
                        }

                        is LongArray -> {
                            if (value.size == 1) {
                                sink.sismember(path, value.first())
                            } else value.forEach { f ->
                                sink.orSismember(path, f)
                            }
                        }

                        is Array<*> -> {
                            if (value.size == 1) {
                                sink.sismember(path, value.first()!!)
                            } else value.forEach { f ->
                                sink.orSismember(path, f!!)
                            }
                        }
                    }
                }
            }

            else -> {
                val message = String.format("%s is not supported for Redis query derivation", part.type)
                throw IllegalArgumentException(message)
            }
        }
        return sink
    }

}