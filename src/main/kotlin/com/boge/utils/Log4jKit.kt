package com.boge.utils

import com.boge.utils.BeanUtils.copy
import com.log4j.appender.MyAppender
import com.log4j.async.AsyncMyLoggerConfig
import com.log4j.config.MyLoggerConfig
import com.log4j.filter.AnyFilter
import com.log4j.filter.RenameFilter
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.RollingFileAppender
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.filter.CompositeFilter
import java.util.*

object Log4jKit {

    /**
     * 将 MyAppender 转换成 RollingFileAppender
     */
    @JvmStatic
    fun convers(loggerConfig: LoggerConfig, appender: Appender?, filter: Filter?): Appender? {
        if (appender is MyAppender) {
            val builder = appender.builder
            var fileName = builder.fileName
            var filePattern = builder.filePattern
            if (fileName.contains("\${") || filePattern.contains("\${")) {
                if (loggerConfig.propertyList != null && loggerConfig.propertyList.isNotEmpty()) {
                    for (it in loggerConfig.propertyList) {
                        fileName = fileName.replace("\${" + it.name + "}", it.value)
                        filePattern = filePattern.replace("\${" + it.name + "}", it.value)
                    }
                }
                var filters: List<RenameFilter>? = null
                if (filter is CompositeFilter) {
                    filters = filter.filtersArray.filterIsInstance<RenameFilter>()
                } else if (filter is AnyFilter) {
                    filters = filter.filtersArray.filterIsInstance<RenameFilter>()
                }
                if (!filters.isNullOrEmpty()) {
                    // 不是null表示有替换名字过滤器
                    for (it in filters) {
                        fileName = fileName.replace("\${" + it.name + "}", it.value)
                        filePattern = filePattern.replace("\${" + it.name + "}", it.value)
                    }
                }
            }

            val appender2 = copy(
                appender.builder,
                RollingFileAppender.Builder::class.java
            )
                .withFileName(fileName)
                .withStrategy(null)
                .withFilePattern(filePattern)

            val policy = appender.getTriggeringPolicy<TriggeringPolicy>()
            if (policy is CompositeTriggeringPolicy) {
                val policies = policy.triggeringPolicies
                val policies2 = arrayOfNulls<TriggeringPolicy>(policies.size)
                for (i in policies.indices) {
                    val triggeringPolicy = policies[i]
                    policies2[i] = copy(triggeringPolicy, triggeringPolicy.javaClass)
                }
                appender2.withPolicy(CompositeTriggeringPolicy.createPolicy(*policies2))
            } else {
                appender2.withPolicy(CompositeTriggeringPolicy.createPolicy(copy(policy, policy.javaClass)))
            }

            var name = loggerConfig.name.replace("\\.".toRegex(), "-")
            if (name.isEmpty() || name.isBlank()) {
                name = System.currentTimeMillis().toString() + "_" + (Random().nextInt(10000, 1000000))
            }
            appender2.setName(appender2.name + "_" + name)

            //            System.out.println("新的： " + appender2.getName());
//            System.out.println("新的： " + appender2.getFileName());
//            System.out.println("新的： " + appender2.getFilePattern());
//            System.out.println("新的： " + appender2.getErrorPrefix());
//            System.out.println("新的： " + appender2.getPolicy());
//            System.out.println("新的： " + appender2.getStrategy());
//            System.out.println("新的： " + appender2.getBufferSize());
//            System.out.println("新的： " + appender2.getFilter());
//            System.out.println("新的： " + Arrays.toString(appender2.getPropertyArray()))

            return appender2.build()
                .also {
                    it.start()
                }
        }
        return appender
    }

    @JvmStatic
    fun isAppenderFiltered(loggerConfig: LoggerConfig, event: LogEvent): Boolean {
        if (loggerConfig is MyLoggerConfig) {
            return loggerConfig.isAppenderFiltered(event)
        } else if (loggerConfig is AsyncMyLoggerConfig) {
            return loggerConfig.isAppenderFiltered(event)
        }
        return true
    }

}