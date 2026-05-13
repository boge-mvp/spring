package com

import com.boge.utils.SystemUtils
import com.log4j.appender.MyAppender
import kotlinx.serialization.encodeToString
import org.apache.logging.log4j.core.appender.RollingFileAppender
import org.junit.jupiter.api.Test
import org.springframework.beans.BeanUtils
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.system.measureNanoTime

class KotlinTest {


    @Test
    fun test() {

        val nums = mutableListOf(0, 1, 2)

        val time = measureNanoTime {
            println(nums.combinations(2))
        }
        val time2 = measureNanoTime {
            println(nums.combinations(2, false))
        }
        println(" $time $time2 ${time - time2}")


    }

    @Test
    fun SystemUtilsTest() {

        println(SystemUtils.runPath)
        println(SystemUtils.applicationHome.source)
        println(SystemUtils.jarName)


    }

    @Test
    fun beanTest() {


        val bu = MyAppender.newBuilder().withFileName("hhh")
        val bu2 = RollingFileAppender.newBuilder().withFileName("rrr")

        BeanUtils.copyProperties(bu, bu2)

        println(JSON.encodeToString(bu))
        println(JSON.encodeToString(bu2))


    }


    @Test
    fun test2() {

        var msg = "".isEmpty { "a" } ?: "b"
        println(msg)

        msg = (Math.random() > .5).ok {
            "对"
        } ?: "错"
        println(msg)


        var a = "发了我开个阿帆我看了飞机".toByteArray()
        println(a.size)
        a = a.deflate()
        println(a.size)
        Path("testData.txt").writeBytes(a)
        println(a.inflate().decodeToString())


        val user = User()
        val result = user.isNotNull {
            "完美"
        }
        println(result)

        println("---------------")
        true.ifOk {
            println("ok")
            "1"
        }.ifOk {
            println("ok2")
        }.ifOk {
            println("ok3")
        }
        false.ifOk {
            println("no")
        }.ifOk {
            println("no2")
        }.ifOk {
            println("no3")
        }


    }

}