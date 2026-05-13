package com

import com.log4j.a.Log4jTestA
import com.log4j.b.Log4jTestB
import com.log4j.c.Log4jTestC
import com.log4j.d.Log4jTestD
import com.log4j.e.Log4jTestE
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.reflect.full.createInstance
import kotlin.test.BeforeTest
import kotlin.test.Test


class Log4jTest {

    @BeforeTest
    fun init() {
        println("init")
        System.setProperty("log4j.configurationFile", Path("log4j2Test.xml").pathString)
    }

    @Test
    fun errorTest(){

        LoggerFactory.getLogger(javaClass).error("测试错误", currentStackTrace())

    }

    @Test
    fun test() {
        println("test")

        // 获取当前的LoggerContext
//        val context = LogManager.getContext(false) as LoggerContext
//        context.stop()
//        context.start(newConfig)
//        context.updateLoggers()


        val log = LoggerFactory.getLogger(javaClass)

        log.trace("trace 测试消息")
        log.info("info 测试消息")
        log.trace("debug 测试消息")
        log.info(MarkerFactory.getMarker("Test"), "测试 Marker")
        log.error("error 测试错误")


//        (0..5).forEach { _ ->

        val methods = listOf(
            Log4jTestA::class, Log4jTestB::class, Log4jTestC::class, Log4jTestD::class, Log4jTestE::class
        )

//        JSON.encodeToString(methods)

        methods.forEachIndexed { _, v ->
//            thread {
                v.createInstance()
//            }
        }


//            Log4jTestA()
//            Log4jTestB()
//            Log4jTestC()
//            Log4jTestD()
//            Log4jTestE()


//        }

        TimeUnit.SECONDS.sleep(3)

//        正确控制台输出
//        2024-02-02 18:23:37,623 [main] TRACE com.Log4jTest : trace 测试消息
//        2024-02-02 18:23:37,627 [main] INFO  com.Log4jTest : info 测试消息
//        2024-02-02 18:23:37,627 [main] TRACE com.Log4jTest : debug 测试消息
//        2024-02-02 18:23:37,628 [main] INFO  com.Log4jTest : 测试 Marker
//        2024-02-02 18:23:37,628 [main] ERROR com.Log4jTest : error 测试错误
//        2024-02-02 18:23:37,631 [main] TRACE com.log4j.a.Log4jTestA : 测试 trace A
//        2024-02-02 18:23:37,635 [main] DEBUG com.log4j.a.Log4jTestA : 测试 debug A
//        2024-02-02 18:23:37,637 [main] WARN  com.log4j.a.Log4jTestA : 测试 warn A
//        2024-02-02 18:23:37,637 [main] ERROR com.log4j.a.Log4jTestA : 测试 error A
//        2024-02-02 18:23:37,638 [main] TRACE com.log4j.b.Log4jTestB : 测试 trace B
//        2024-02-02 18:23:37,639 [main] WARN  com.log4j.b.Log4jTestB : 测试 warn B
//        2024-02-02 18:23:37,639 [main] ERROR com.log4j.b.Log4jTestB : 测试 error B
//        2024-02-02 18:23:37,640 [main] TRACE com.log4j.c.Log4jTestC : 测试 trace C
//        2024-02-02 18:23:37,641 [main] DEBUG com.log4j.c.Log4jTestC : 测试 debug C
//        2024-02-02 18:23:37,641 [main] INFO  com.log4j.c.Log4jTestC : 测试 info C
//        2024-02-02 18:23:37,641 [main] INFO  com.log4j.c.Log4jTestC : 测试 Marker C
//        2024-02-02 18:23:37,648 [main] TRACE com.log4j.d.Log4jTestD : 测试 trace D
//        2024-02-02 18:23:37,652 [main] DEBUG com.log4j.e.Log4jTestE : 测试 debug E
//        2024-02-02 18:23:37,652 [main] INFO  com.log4j.e.Log4jTestE : 测试 info E
//        2024-02-02 18:23:37,652 [main] INFO  com.log4j.e.Log4jTestE : 测试 Marker E
//        2024-02-02 18:23:37,652 [main] WARN  com.log4j.e.Log4jTestE : 测试 warn E
//        2024-02-02 18:23:37,652 [main] ERROR com.log4j.e.Log4jTestE : 测试 error E


    }

}