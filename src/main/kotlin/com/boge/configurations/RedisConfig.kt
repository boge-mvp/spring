package com.boge.configurations

import com.boge.RedisMapper
import com.boge.converter.ByteArrayToTimestampConverter
import com.boge.entity.RedisYaml
import com.boge.redis.RedisKeyValueAdapter
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.ifNotBlank
import com.isNotNull
import com.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionService
import org.springframework.data.keyvalue.core.KeyValueTemplate
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.RedisKeyValueTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.convert.RedisCustomConversions
import org.springframework.data.redis.core.mapping.RedisMappingContext
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.stereotype.Component


/**
 *
 * @see org.springframework.data.redis.repository.configuration.RedisRepositoryConfigurationExtension
 * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
 * @see org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration
 *
 */
@Configuration
class RedisConfig : CachingConfigurer {

    @Value("\${spring.profiles.active}")
    val env = ""

    @Bean
    fun redisMapper(stringRedisTemplate: StringRedisTemplate): RedisMapper {
        return RedisMapper(stringRedisTemplate)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory, conversionService: ConversionService): RedisTemplate<String, Any> {
        //1.构建RedisTemplate对象
        val redisTemplate = RedisTemplate<String, Any>()
        //2.设置连接工厂
        redisTemplate.connectionFactory = redisConnectionFactory

        //3.定义序列化方式(在这里选择jackson)
        val objectMapper = ObjectMapper()
        //设置要序列化的域(通过所有get方法获取所有属性名以及值)
        objectMapper.setVisibility(
            PropertyAccessor.ALL,
            JsonAutoDetect.Visibility.ANY
        ) //any表示任意级别访问修饰符修饰的属性 private,public,protected

        //启动输入域检查(类不能是final修饰的),并基于json串中的key将其值存储到pojo对象
        objectMapper.activateDefaultTyping(
            objectMapper.polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        )
        val redisSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        // 设置默认序列化转换器
        redisTemplate.setDefaultSerializer(redisSerializer)
        //4.设置RedisTemplate的序列化
        redisTemplate.keySerializer = RedisSerializer.string()
        redisTemplate.valueSerializer = redisSerializer
        redisTemplate.hashKeySerializer = RedisSerializer.string()
        redisTemplate.hashValueSerializer = redisSerializer
        //spring规范中假如修改bean对象的默认特性,建议调用一下afterPropertiesSet()
        redisTemplate.afterPropertiesSet()
        return redisTemplate
    }

    /**
     * 自己定义CacheManager,序列化方式,key的有效时长
     * 下面这段代码是可以不写的.(不写就用默认的)
     */
    @Bean
    @ConditionalOnExpression("\${config.redis.enabledCache:true}") // 配置为true就初始化
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory, redisYaml: RedisYaml): RedisCacheManager {
        logger.info("enabled redis cache manager")
        //初始化一个RedisCacheWriter
        val redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory)
        //设置CacheManager的值序列化方式为json序列化

        val objectMapper = ObjectMapper()
        //设置要序列化的域(通过所有get方法获取所有属性名以及值)
        objectMapper.setVisibility(
            PropertyAccessor.ALL,
            JsonAutoDetect.Visibility.ANY
        ) //any表示任意级别访问修饰符修饰的属性 private,public,protected

        //启动输入域检查(类不能是final修饰的),并基于json串中的key将其值存储到pojo对象
        objectMapper.activateDefaultTyping(
            objectMapper.polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        )

        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        val pair = RedisSerializationContext.SerializationPair
            .fromSerializer(jsonSerializer)
        val defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(pair)
            .computePrefixWith { "${redisYaml.keyPrefix.ifNotBlank { redisYaml.keyPrefix + ":" }}$it:" }

        //设置要保存多久
        redisYaml.entryTtl.isNotNull {
            defaultCacheConfig.entryTtl(this)
        }

        //初始化RedisCacheManager
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheWriter(redisCacheWriter)
            .cacheDefaults(defaultCacheConfig)
            .build()
    }









    @Bean
    fun redisMappingContext(redisYaml: RedisYaml): RedisMappingContext {
        val mappingContext = RedisMappingContext()
        // 在这里进行RedisMappingContext的配置
        // 可以添加自定义的Converter、IndexResolver等
        mappingContext.setKeySpaceResolver { type ->
            val prefix = type.getAnnotation(RedisHash::class.java)?.value
            "${redisYaml.keyPrefix.ifNotBlank { "${redisYaml.keyPrefix}:${env.ifNotBlank { "$env:" }}" }}$prefix"
        }
        return mappingContext
    }

    @Bean
    fun redisCustomConversions(): RedisCustomConversions {
        val converters = listOf(ByteArrayToTimestampConverter())
        return RedisCustomConversions(converters)
    }

    /**
     * 为 redis repository 配置自定义的key value
     * @EnableRedisRepositories 中有定义这个属性
     */
    @Bean
    fun redisKeyValueTemplate(redisTemplate: RedisTemplate<String, Any>, redisMappingContext: RedisMappingContext, redisCustomConversions: RedisCustomConversions): KeyValueTemplate {
        val adapter = RedisKeyValueAdapter(redisTemplate, redisMappingContext, redisCustomConversions)
        return RedisKeyValueTemplate(adapter, redisMappingContext)
    }













    @Bean
    fun container(connectionFactory: RedisConnectionFactory, listener: KeyExpirationListener): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)

        // 配置消息监听器适配器，将自定义的消息监听器方法与消息处理器关联
        val adapter = MessageListenerAdapter(listener, "onMessage")

//        adapter.setSerializer(connectionFactory.connection.getConverter().getSerializationPair().getValueSerializer())

        // 订阅键删除事件（显式删除）
        container.addMessageListener(adapter, ChannelTopic("__keyevent@0__:del"))

        // 订阅键过期事件
        container.addMessageListener(adapter, ChannelTopic("__keyevent@0__:expired"))

        return container
    }
}

@Component
class KeyExpirationListener(val redisMapper: RedisMapper) : MessageListener {

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val channel = message.channel.decodeToString()
        val key = message.body.decodeToString()

        if (channel == "__keyevent@0__:del") { // "del"事件表示键被显式删除
            logger.info("Key deleted: $key")
            // 在此处添加针对键删除的实际业务逻辑处理
        } else if (channel == "__keyevent@0__:expired") { // "expired"事件表示键因过期被删除
            logger.info("Key expired: $key")
            // 在此处添加针对键过期的实际业务逻辑处理

            val keys = key.split(":")
            val keyName = keys.take(keys.size - 1).joinToString(":")
            val value = keys[keys.size - 1]
            redisMapper.redis.opsForSet().remove(keyName, value)
            logger.info("remove $key $keyName $value ")
        }
    }
}