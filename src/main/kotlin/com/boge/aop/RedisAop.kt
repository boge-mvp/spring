package com.boge.aop

import com.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component


@Component
@Aspect
class RedisAop {

//    @Pointcut("execution(* org.springframework.data.redis.connection.RedisConnection.*(..))")
//    fun redisOperations() {
//    }
//
//    @Around("redisOperations()")
//    fun printRedisCommand(joinPoint: ProceedingJoinPoint): Any {
//        val methodName = joinPoint.signature.name
//        println("*************************************************** $methodName *****-------------------------------")
//        val args = joinPoint.args
//        if (args != null && args.isNotEmpty() && args[0] is ByteArray) {
//            val command = args[0] as ByteArray
//            logger.info("Redis Command: " + String(command))
//        }
//        return joinPoint.proceed()
//    }






}