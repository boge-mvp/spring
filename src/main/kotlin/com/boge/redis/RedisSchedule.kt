package com.boge.redis

import com.boge.RedisMapper
import com.boge.condition.JunitCondition
import com.boge.entity.RedisYaml
import com.ifOk
import com.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Conditional
import org.springframework.data.redis.util.ByteUtils
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Conditional(JunitCondition::class)
@ConditionalOnClass(EnableScheduling::class)
@Component
class RedisSchedule(val redisMapper: RedisMapper, var redisYaml: RedisYaml) {

    @Value("\${spring.profiles.active}")
    val env = ""

    /**
     * 检查是否有过期资源被删除 但是id没有删除
     */
    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    fun checkRedisRepositoryExpired() {

        logger.info("checkRedis ------------")


        redisYaml.validationFails.ifOk {

            val findKeys = this.map {
                "${redisYaml.keyPrefix}:$env:$it"
            }
//            println("find ${findKeys.size} $findKeys")
            redisMapper.redis.execute {

                findKeys.forEach { key ->
                    val lists = it.setCommands().sMembers(key.toByteArray())
//                    println("查询结果 ${lists?.size}")
                    lists?.forEach { v ->
                        val findKey = ByteUtils.concatAll("$key:".toByteArray(), v)
                        println(findKey.decodeToString())
                        // TTL（以毫秒为单位）。
                        // -1 密钥存在但没有相关的到期时间。
                        // -2 键不存在。
                        val ttl = it.keyCommands().pTtl(findKey)
//                        println("结果  $ttl")
                        if (ttl == null || ttl == -2L) {
                            it.setCommands().sRem(key.toByteArray(), v)
                            logger.info("$key 需要删除value: ${v.decodeToString()}")
                        }
                    }


                }
            }

        }


    }


}