package com.boge.configurations

import com.baomidou.mybatisplus.core.injector.AbstractMethod
import com.baomidou.mybatisplus.core.metadata.TableInfo
import com.baomidou.mybatisplus.core.toolkit.ClassUtils
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor
import com.boge.ITableName
import com.boge.entity.HttpYaml
import com.boge.entity.MybatisInnerInterceptorYaml
import com.boge.interceptors.MybatisSqlOutInterceptor
import com.boge.interceptors.TableNameInnerInterceptor
import com.logger
import com.ok
import icu.mhb.mybatisplus.plugln.injector.JoinDefaultSqlInjector
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import jakarta.annotation.Resource
import org.apache.ibatis.binding.MapperMethod.ParamMap
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter


@Configuration
class MyBatisPlusConfig : JoinDefaultSqlInjector() {
    override fun getMethodList(mapperClass: Class<*>?, tableInfo: TableInfo): List<AbstractMethod> {
        return super.getMethodList(mapperClass, tableInfo)
    }
}

@Configuration
class Configurations {

    @Resource
    lateinit var mybatisInnerInterceptorYaml: MybatisInnerInterceptorYaml

    @Resource
    lateinit var httpYaml: HttpYaml

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOriginPattern("*")
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }

    /**
     * 用于HttpUtils异步使用的scheduler
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpScheduler(httpSchedulerExecutor: ThreadPoolTaskExecutor): Scheduler {
        return Schedulers.from(httpSchedulerExecutor)
    }

    /**
     * 用于HttpUtils的异步请求的线程池
     */
    @Bean
    fun httpSchedulerExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            this.corePoolSize = 1 // 设置核心线程数 真实最大干事的线程数量
            this.queueCapacity = 10 // 设置队列容量 满了才增加线程
            this.setThreadNamePrefix("http-scheduler-")
            if (httpYaml.virtualThread)
                this.setThreadFactory(
                    Thread.ofVirtual()
                        .name("http-scheduler-virtual-", 0)
                        .uncaughtExceptionHandler { t, e ->
                            logger.error("${t.name} 执行错误", e)
                        }
                        .factory()
                )
            this.initialize()
        }
    }

    @Bean
    fun mybatisPlusInterceptor(tableNameInnerInterceptor: TableNameInnerInterceptor): MybatisPlusInterceptor {
        return MybatisPlusInterceptor().apply {
            // 分页插件
            mybatisInnerInterceptorYaml.enabledPage.ok {
                addInnerInterceptor(PaginationInnerInterceptor(mybatisInnerInterceptorYaml.pageDbType))
            }
            mybatisInnerInterceptorYaml.enabledTableName.ok {
                addInnerInterceptor(tableNameInnerInterceptor)
            }
            mybatisInnerInterceptorYaml.sqlLog.enabledSqlLog.ok {
                addInnerInterceptor(MybatisSqlOutInterceptor(mybatisInnerInterceptorYaml.sqlLog.sqlLogMarker, tableNameInnerInterceptor))
            }
            mybatisInnerInterceptorYaml.interceptor.forEach {
                addInnerInterceptor(ClassUtils.newInstance(it))
            }
        }
    }

    @Bean
    fun tableNameInnerInterceptor(): TableNameInnerInterceptor {
        return TableNameInnerInterceptor(
            tableNameHandler = { sql, name, boundSql ->
                var tableName = name
                if (boundSql != null) {
                    // 获取参数方法
                    when (val obj = boundSql.parameterObject) {
                        is ITableName -> tableName = obj.onTableName(sql, name, boundSql)
                        is ParamMap<*> -> {
                            if (obj.containsKey("ew")) {
                                val value = obj["ew"]
                                if (value is KtQueryWrapper<*> && value.entity != null && value.entity is ITableName) {
                                    tableName = (value.entity as ITableName).onTableName(sql, name, boundSql)
                                }
                            } else if (obj.containsKey("param1")) {
                                val value = obj["param1"]
                                if (value is KtQueryWrapper<*> && value.entity != null && value.entity is ITableName) {
                                    tableName = (value.entity as ITableName).onTableName(sql, name, boundSql)
                                }
                            }
                        }
                    }
                }
                tableName
            }
        )
    }

}