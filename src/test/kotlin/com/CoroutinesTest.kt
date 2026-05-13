package com

import com.boge.utils.CoroutinesUtils
import kotlinx.coroutines.Dispatchers
import kotlin.system.measureTimeMillis
import kotlin.test.Test

/**
 * 协程测试类
 */
class CoroutinesTest {


    @Test
    fun testDispatchers_IO() {

        val totalTime = measureTimeMillis {
            println("测试第1个")
            CoroutinesUtils.runGlobalAsync((0..3).toList(), Dispatchers.IO, {
                println("1.开始 $it ${Thread.currentThread().name}")
                val start = System.currentTimeMillis()
                val time = measureTimeMillis {
                    while ((System.currentTimeMillis() - start) / 1000 < (it + 5)) {
                    }
                }
                println("1.结束 $it ${Thread.currentThread().name} $time")
            })  {
                println("第1个全部完成 ${Thread.currentThread().name}")
            }

            println("测试第2个")

            CoroutinesUtils.runAsync((0..3).toList(), Dispatchers.IO, {
                println("2.开始 $it ${Thread.currentThread().name}")
                val start = System.currentTimeMillis()
                val time = measureTimeMillis {
                    while ((System.currentTimeMillis() - start) / 1000 < (it + 5)) {
                    }
                }
                println("2.结束 $it ${Thread.currentThread().name} $time")
            }) {
                println("第2个全部完成 ${Thread.currentThread().name}")
            }

            println("测试第3个")
            CoroutinesUtils.runSync((0..3).toList(), Dispatchers.IO) {
                println("3.开始 $it ${Thread.currentThread().name}")
                val start = System.currentTimeMillis()
                val time = measureTimeMillis {
                    while ((System.currentTimeMillis() - start) / 1000 < (it + 5)) {
                    }
                }
                println("3.结束 $it ${Thread.currentThread().name} $time")
            }
        }

        println("执行结束 $totalTime ${Thread.currentThread().name}")

    }

    @Test
    fun testDispatchers_Default() {

        val totalTime = measureTimeMillis {
            println("测试第1个")
            CoroutinesUtils.runGlobalAsync((0..3).toList(), Dispatchers.Default, {
                println("1.开始 $it ${Thread.currentThread().name}")
                val start = System.currentTimeMillis()
                val time = measureTimeMillis {
                    while ((System.currentTimeMillis() - start) / 1000 < (it + 5)) {
                    }
                }
                println("1.结束 $it ${Thread.currentThread().name} $time")
            })

            println("测试第2个")

            CoroutinesUtils.runAsync((0..3).toList(), Dispatchers.Default, {
                println("2.开始 $it ${Thread.currentThread().name}")
                val start = System.currentTimeMillis()
                val time = measureTimeMillis {
                    while ((System.currentTimeMillis() - start) / 1000 < (it + 5)) {
                    }
                }
                println("2.结束 $it ${Thread.currentThread().name} $time")
            })

            println("测试第3个")
            CoroutinesUtils.runSync((0..3).toList(), Dispatchers.Default) {
                println("3.开始 $it ${Thread.currentThread().name}")
                val start = System.currentTimeMillis()
                val time = measureTimeMillis {
                    while ((System.currentTimeMillis() - start) / 1000 < (it + 5)) {
                    }
                }
                println("3.结束 $it ${Thread.currentThread().name} $time")
            }
        }

        println("执行结束 $totalTime ${Thread.currentThread().name}")

    }


}