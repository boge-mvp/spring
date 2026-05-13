package com.boge.redis

import org.springframework.data.keyvalue.core.KeyValueOperations
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactoryBean
import org.springframework.data.redis.repository.query.RedisPartTreeQuery
import org.springframework.data.redis.repository.support.RedisRepositoryFactory
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.RepositoryQuery
import org.springframework.data.repository.query.parser.AbstractQueryCreator

/**
 * @see org.springframework.data.redis.repository.support.RedisRepositoryFactoryBean
 */
class RedisRepositoryFactoryBean<T : Repository<S, ID>, S, ID>(repositoryInterface: Class<T>) :
    KeyValueRepositoryFactoryBean<T, S, ID>(repositoryInterface) {

    init {
        setQueryType(RedisPartTreeQuery::class.java)
    }

    override fun createRepositoryFactory(
        operations: KeyValueOperations,
        queryCreator: Class<out AbstractQueryCreator<*, *>>,
        repositoryQueryType: Class<out RepositoryQuery>
    ): KeyValueRepositoryFactory {
        return RedisRepositoryFactory(operations, RedisQueryCreator::class.java, repositoryQueryType)
    }

}