package com

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.web.context.support.GenericWebApplicationContext
import kotlin.reflect.KClass

object Consts {

    @JvmStatic
    lateinit var context: ConfigurableApplicationContext

    /**
     * 获取通用的ApplicationContext
     */
    @JvmStatic
    val contextGeneric
        get() = (context is GenericWebApplicationContext).ok { context as GenericWebApplicationContext }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> getBean(requiredType: String): T? {
        return if (context.isActive && context.isRunning && context.containsBean(requiredType))
            context.getBean(requiredType) as T? else null
    }

    @JvmStatic
    fun <T> getBean(requiredType: Class<T>): T? {
        return if (context.isActive && context.isRunning && context.getBeanNamesForType(requiredType).size == 1)
            context.getBean(requiredType) else null
    }

    @JvmStatic
    fun <T : Any> getBean(requiredType: KClass<T>): T? {
        return getBean(requiredType.java)
    }

}
