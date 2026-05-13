package com.boge.configurations

import com.boge.entity.RedisYaml
import com.logger
import jakarta.annotation.Resource
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory

@Configuration
class LockProviderConfig {

    @Value("\${spring.profiles.active}")
    val env = ""

    @Resource
    lateinit var redisYaml: RedisYaml

    @Bean
    fun lockProvider(connectionFactory: RedisConnectionFactory): LockProvider {
        logger.info("配置 lockProvider: $env ${redisYaml.keyPrefix}")
        //环境变量 -需要区分不同环境避免冲突，如dev环境和test环境，两者都部署同一个服务器时，只有一个实例进行，此时会造成相关环境未启动情况
        return RedisLockProvider(connectionFactory, env, redisYaml.keyPrefix)
    }

}
