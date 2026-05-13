package com

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ObservableTest {

    private lateinit var latch: CountDownLatch

    @Test
    fun test() {
        val merge = mutableListOf<Observable<String>>()
        for (i in 0..<10) {
            merge.add(TestClass(i).create().subscribeOn(Schedulers.io()))
        }

        println("${Thread.currentThread().name} 测试用例=${merge.size}个")
        // 创建一个 CountDownLatch 对象，用于等待所有请求完成
        latch = CountDownLatch(merge.size)

        // 将所有 Observable 对象合并成一个 Observable 对象
        val allObservables = Observable.mergeDelayError(merge, 3)

        // 订阅合并后的 Observable 对象，并指定观察者在单独的线程执行
        allObservables
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .subscribe(observer)


        // 等待所有请求完成
        latch.await()

        // 执行后续任务
        println("${Thread.currentThread().name} Continue to execute the following tasks...")

    }

    // 创建一个 Observer 对象，用于处理所有请求的结果
    private val observer = object : Observer<String> {
        override fun onSubscribe(d: Disposable) {}

        override fun onNext(t: String) {
            // 处理响应数据
            println("${Thread.currentThread().name} Received response: $t")
            latch.countDown()
        }

        override fun onError(e: Throwable) {
            // 处理错误情况
            println("${Thread.currentThread().name} Error: ${e.message}")
            latch.countDown() // 减少 CountDownLatch 的计数器
        }

        override fun onComplete() {
            // 请求完成
            println("${Thread.currentThread().name} All requests completed")
//                latch.countDown() // 减少 CountDownLatch 的计数器
        }
    }


    class TestClass(private val tempId:Int) {
        fun create(): Observable<String> {
            return Observable.fromCallable {
                println("执行时间=${LocalDateTime.now()}")
                TimeUnit.MILLISECONDS.sleep(Random.nextLong(100, 1000))
                if (Random.nextBoolean()) {
//                    throw Exception("Error occurred for $tempId") // 模拟请求错误
                    "${Thread.currentThread().name} Response 失败 $tempId"
                } else {
                    "${Thread.currentThread().name} Response 成功 $tempId"
                }
            }
        }

    }


}