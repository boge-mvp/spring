package com.boge.listeners

import com.Consts
import com.logger
import org.springframework.boot.ConfigurableBootstrapContext
import org.springframework.boot.SpringApplicationRunListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import java.time.Duration

class SpringRunListener : SpringApplicationRunListener {

    override fun starting(bootstrapContext: ConfigurableBootstrapContext?) {
        super.starting(bootstrapContext)        // 在应用程序开始运行之前执行的操作
        logger.debug("starting")
    }

    override fun environmentPrepared(
        bootstrapContext: ConfigurableBootstrapContext?,
        environment: ConfigurableEnvironment?
    ) {
        super.environmentPrepared(bootstrapContext, environment)        // 在环境准备阶段执行的操作
        logger.debug("environmentPrepared")
    }

    override fun contextPrepared(context: ConfigurableApplicationContext?) {
        super.contextPrepared(context)        // 在上下文准备阶段执行的操作
        logger.debug("contextPrepared: {}", context)
        Consts.context = context!!
    }

    override fun contextLoaded(context: ConfigurableApplicationContext?) {
        super.contextLoaded(context)        // 在上下文加载阶段执行的操作
        logger.debug("contextLoaded")
    }

    override fun started(context: ConfigurableApplicationContext?, timeTaken: Duration?) {
        super.started(context, timeTaken)        // 在应用程序启动完成后执行的操作
        logger.debug("started")
    }

    override fun ready(context: ConfigurableApplicationContext?, timeTaken: Duration?) {
        super.ready(context, timeTaken)        // 在应用程序运行阶段执行的操作
        logger.debug("ready")
    }

    override fun failed(context: ConfigurableApplicationContext?, exception: Throwable?) {
        super.failed(context, exception)        // 在应用程序运行失败时执行的操作
        logger.debug("failed")
    }

}