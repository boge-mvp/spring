package com.boge.configurations

import com.boge.redis.RedisExtendRepositoryImpl
import com.boge.redis.RedisRepositoryFactoryBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.repository.configuration.RedisRepositoryConfigurationExtension
import org.springframework.data.repository.config.RepositoryConfigurationExtension

/**
 * @see org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
 */

@AutoConfiguration(after = [RedisAutoConfiguration::class])
@ConditionalOnClass(EnableRedisRepositories::class)
@ConditionalOnBean(RedisConnectionFactory::class)
@ConditionalOnProperty(
    prefix = "spring.data.redis.repositories",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@ConditionalOnMissingBean(RedisRepositoryFactoryBean::class)
@Import(RedisRepositoriesRegistrar::class)
class RedisRepositoriesAutoConfiguration

/**
 * @see org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesRegistrar
 */
class RedisRepositoriesRegistrar : AbstractRepositoryConfigurationSourceSupport() {

    override fun getAnnotation(): Class<out Annotation> {
        return EnableRedisRepositories::class.java
    }

    override fun getConfiguration(): Class<*> {
        return EnableRedisRepositoriesConfiguration::class.java
    }

    override fun getRepositoryConfigurationExtension(): RepositoryConfigurationExtension {
        return RedisRepositoryConfigurationExtension()
    }

    @EnableRedisRepositories(
        repositoryBaseClass = RedisExtendRepositoryImpl::class,
        repositoryFactoryBeanClass = RedisRepositoryFactoryBean::class
    )
    private class EnableRedisRepositoriesConfiguration
}


