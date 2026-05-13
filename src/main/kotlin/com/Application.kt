package com

import icu.mhb.mybatisplus.plugln.interceptor.JoinInterceptor
import icu.mhb.mybatisplus.plugln.interceptor.JoinInterceptorConfig
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(
    exclude = [
        RedisRepositoriesAutoConfiguration::class // 取消框架本身的自动注册
    ]
)
@EnableScheduling
@EnableAsync
// 开启定时任务锁，默认设置锁最大占用时间为20分钟  最小30s  例如：PT30S  30s时间设置，单位可以是S,M,H
@EnableSchedulerLock(defaultLockAtMostFor = "PT20M", defaultLockAtLeastFor = "PT1M")
@EnableRetry
@EnableCaching
// 将 exposeProxy 属性设置为 true，可以将当前的代理对象暴露给 AopContext.currentProxy() 方法，从而在同一个线程中获取到当前的代理对象。这对于在同一个类中的方法自调用时触发事务切面非常有用。
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableConfigurationProperties
class Application : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
//        log.debug("")
        init()
    }

    protected fun init() {}

}


@EnableTransactionManagement
@Import(JoinInterceptor::class, JoinInterceptorConfig::class)
class ServerApplication : Application()