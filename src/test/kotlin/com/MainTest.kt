package com

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
import com.boge.utils.SystemUtils
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.outputStream

@SpringBootApplication(exclude = [
    DataSourceAutoConfiguration::class,
    MybatisPlusAutoConfiguration::class,
    DruidDataSourceAutoConfigure::class
])
class MainTest : Application() {

    override fun init() {
        super.init()
        log.info(Consts.context.toString())
    }

}

@Component
class ExcludeClassBeanPostProcessor : BeanPostProcessor {

    val out by lazy {
        FileOutputStream(File(SystemUtils.dir, "bean.txt"))
    }
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        out.write("$beanName ${bean::class.simpleName}\n".toByteArray())
        return super.postProcessAfterInitialization(bean, beanName)
    }

}