package com

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test

class VirtualThreadTest {


    @Test
    fun test() {

        println("开始  ${Thread.currentThread()}")
        val executors = Executors.newVirtualThreadPerTaskExecutor()
        val executors2 = Executors.newVirtualThreadPerTaskExecutor()
        val executors3 = Executors.newVirtualThreadPerTaskExecutor()
        (0..10).forEach {
            executors.execute {
                println("${Thread.currentThread()}_ ${Thread.currentThread().isVirtual}")
                val end = read(it + 1)
                println("${Thread.currentThread()}等待结束 $end")
            }
        }
        println("结束  ${Thread.currentThread()}")
        // 关闭执行器以确保所有任务完成并释放资源
        executors.shutdown()
        executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)

    }


    @Test
    fun testT() {


        println("开始  ${Thread.currentThread()}")


        thread {
            println("新线程开始  ${Thread.currentThread()}")
            val executors = Executors.newVirtualThreadPerTaskExecutor()

            (0..10).forEach {
                executors.execute {
                    println("${Thread.currentThread()}_ ${Thread.currentThread().isVirtual}")
                    val end = read(it + 1)
                    println("${Thread.currentThread()}等待结束 $end")
                }
            }

            println("新线程结束  ${Thread.currentThread()}")

        }

        println("结束  ${Thread.currentThread()}")


        TimeUnit.SECONDS.sleep(20)

        // 关闭执行器以确保所有任务完成并释放资源
//        executors.shutdown()
//        executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)


    }

    @Test
    fun testCompletableFuture() {


        println("开始  ${Thread.currentThread()}")

        val list = mutableListOf<CompletableFuture<String>>()


        val f = Executors.newVirtualThreadPerTaskExecutor()
            .submit {

        }



        CompletableFuture.runAsync {  }


        (0..10).forEach {
            val a = CompletableFuture.supplyAsync {
                println("${Thread.currentThread()}_ ${Thread.currentThread().isVirtual}")
                val end = read(it + 1)
                println("${Thread.currentThread()}等待结束 $end")
                "$it 结束了"
            }
            list.add(a)
        }

        CompletableFuture.allOf(*list.toTypedArray())
            .thenApplyAsync {
                println("thenApplyAsync $it ")
            }
            .thenRun {
                println("thenRun ")
            }
            .whenCompleteAsync { t, u ->


                println("来了 $u ${Thread.currentThread()}")
            }
            .get()


        println("结束  ${Thread.currentThread()}")


        TimeUnit.SECONDS.sleep(20)

        // 关闭执行器以确保所有任务完成并释放资源
//        executors.shutdown()
//        executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)


    }

    @Test
    fun testForkJoinPool() {
        ForkJoinPool.commonPool()
            .execute {
            println(" ${Thread.currentThread()} ${Thread.currentThread().isVirtual} 执行了*** ")
        }
    }


    private fun read(index: Int): Int {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < index * 500) {
            Thread.sleep(10)
        }
        return index
    }


}