package com.boge.utils

import com.Consts
import com.boge.IRequestData
import com.boge.entity.RequestResult
import com.log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import retrofit2.Retrofit
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * 批量https请求
 */
object HttpUtil {

    internal val defaultScheduler: Scheduler by lazy {
        Consts.getBean("httpScheduler")!!
    }


    val retrofit: Retrofit by lazy { Consts.context.getBean(Retrofit::class.java) }

    /**
     * 任务集合
     * 泛型T是最终返回结果对象  D是附带的对象
     * @param method 执行的网络请求
     * @param taskList 任务集合 参数可以是 List<String> 或 List<IRequestData<D>>
     *
     * ```
     * 使用方法：
     * 1.声明网络请求接口
     * @GET
     * fun getResult(@Url url: String): Observable<String>
     * 2.设置参数数组
     * val urls = listOf(
     *     "https://xxx",
     *     "https://xxx"
     * )
     * 或转换成List<IRequestData>
     * val urlsD = urls.mapIndexed { index, it ->
     *     object : IRequestData<String> {
     *         override val param: Array<*> = arrayOf(it)
     *         override val data: String = "$index ---------- " + it.substringAfterLast("/")
     *     }
     * }
     * 3.执行网络请求
     * val httpNet = HttpUtil
     *     .batchGet<String, String>(IHttp::getResult, urlsD)
     *     .async()
     *     .httpScheduler(Schedulers.computation())
     *     .retry(3)
     * 4.处理请求结果
     * httpNet.subscribe({
     *     log.info("完成进度：${httpNet.total - httpNet.count + 1} / ${httpNet.total}")
     *     if (it.result != null) {
     *         log.info("Received response: ${it.data} ${it.result}")
     *     } else if (it.error != null) {
     *         val err = it.error
     *         if (err is HttpException) {
     *             log.error("http error=${it.data} ${err.response()?.raw()?.request?.url}", err)
     *         } else {
     *             log.error("error", err)
     *         }
     *     }
     * }, {
     *     log.info("All requests completed")
     * })
     * // 等待所有请求结束
     * httpNet.await()
     *
     * ```
     *
     */
    inline fun <reified T : Any, reified D> batchGet(method: KFunction<*>, taskList: List<*>): HttpNet<T, D> {
        val javaMethod = method.javaMethod!!
        return HttpNet(javaMethod, javaMethod.declaringClass, taskList)
    }

}

@Suppress("UNCHECKED_CAST")
class HttpNet<T : Any, D>(
    private val method: Method,
    private val declaringClass: Class<*>,
    private var taskList: List<*>,
    /**
     * 是否使用线程
     */
    private var isAsync: Boolean = false,
    /**
     * 是否使用协程
     */
    private var isCoroutines: Boolean = false
) {

    private lateinit var latch: CountDownLatch
    private var scheduler: Scheduler? = null
    private var httpScheduler: Scheduler? = null

    /**
     * 自定义带参数的任务
     */
    private var entitys: List<IRequestData<D, Any>>? = null

    /**
     * 可以同时订阅的 ObservableSource 的最大数量
     *
     * 默认 Int.MAX_VALUE
     */
    private var maxConcurrency = Int.MAX_VALUE

    /** 重试次数 */
    private var retryNum = 0L

    init {
        // 要么是IRequestData 对象 要么是参数数组
        if (taskList.isNotEmpty()) {
            val first = taskList.first() // 取出第一个任务
            if (first !is Array<*>) { // 不是数组  单个任务
                if (first is IRequestData<*, *>) {
                    entitys = taskList as List<IRequestData<D, Any>>
                } else {
                    // 参数数组
                    taskList = taskList.map {
                        arrayOf(it)
                    }
                }
            }
        }
    }

    /**
     * 可以同时订阅的 ObservableSource 的最大数量
     *
     * 默认 Int.MAX_VALUE
     */
    fun maxConcurrency(value: Int): HttpNet<T, D> {
        this.maxConcurrency = value
        return this
    }

    /**
     * 激活异步
     */
    fun async(): HttpNet<T, D> {
        this.isAsync = true
        return this
    }

    fun coroutines(): HttpNet<T, D> {
        this.isCoroutines = true
        return this
    }

    /**
     * 请求异步线程
     * @param scheduler
     */
    fun httpScheduler(scheduler: Scheduler?): HttpNet<T, D> {
        this.httpScheduler = scheduler
        return this
    }

    /**
     * Observable执行线程
     */
    fun scheduler(scheduler: Scheduler?): HttpNet<T, D> {
        this.scheduler = scheduler
        return this
    }

    /**
     * Observable执行失败 重试次数 默认0
     */
    fun retry(retryNum: Long): HttpNet<T, D> {
        this.retryNum = retryNum
        return this
    }


    /**
     * 加载的总元素
     */
    val total: Int
        get() = entitys?.size ?: taskList.size

    /**
     * 返回当前计数 为0表示全部完成
     */
    val count: Long
        get() = latch.count

    fun subscribe(
        onNext: Consumer<RequestResult<T, D>>? = null,
        onComplete: Action? = null
    ): Disposable? {
        val request = entitys?.map { it.param } ?: taskList as List<Array<*>>
        if (request.isEmpty()) {
            return null
        }
        val merge = ArrayList<Observable<RequestResult<T, D>>>(request.size)
        latch = CountDownLatch(request.size)

        request.forEachIndexed { index, items ->
            val service = HttpUtil.retrofit.create(declaringClass)
            val response = (method.invoke(service, *items) as Observable<T>).let {
                if (this.isAsync)
                    it.subscribeOn(httpScheduler ?: HttpUtil.defaultScheduler)
                        .retry(this.retryNum)
                else it.retry(this.retryNum)
            }.let { observable ->
                observable
                    .materialize()
                    .map {
                        val tempRequest = RetrofitUtils.getCall(observable)?.request()
                        when {
                            it.isOnNext -> RequestResult(1, it.value, it.error, entitys?.get(index)?.data, tempRequest)

                            it.isOnError -> RequestResult<T, D>(2,null, it.error, entitys?.get(index)?.data, tempRequest)

                            else -> RequestResult<T, D>(3,null, it.error, entitys?.get(index)?.data, tempRequest)
                        }
                    }
                    .doOnTerminate {
                        log.debug("*****************************  doOnTerminate  *********************************")
                        latch.countDown()
                    }
            }
            merge.add(response)
        }

        return Observable.merge(merge, maxConcurrency).let {
            // 非异步的情况下 改动作在异步执行
            if (!isAsync) it.subscribeOn(scheduler ?: HttpUtil.defaultScheduler) else it
        }
            .doOnEach {
                when {
                    it.isOnNext -> onNext?.accept(it.value!!)
                    it.isOnComplete -> onComplete?.run()
                }
            }
            .subscribe()
    }

    /**
     * 使当前线程等待，直到闩锁倒计时为零，除非线程 [中断][Thread.interrupt].
     *
     * 如果当前计数为零，则此方法将立即返回。
     *
     * 如果当前计数大于零，则当前线程将出于线程调度目的被禁用并处于休眠状态，直到发生以下两种情况之一：
     *  * 由于调用方法 countDown，计数达到零
     *  * 其他一些线程[中断][Thread.interrupt] 当前线程。
     *
     * 如果当前线程：
     * * 在进入此方法时设置了中断状态;或
     * * 在等待时 被[中断][Thread.interrupt] ，
     *
     * 然后 [InterruptedException] 被抛出，当前线程的中断状态被清除。
     *
     * @throws InterruptedException 如果当前线程在等待时中断
     */
    fun await() {
        return latch.await()
    }

    /**
     * 使当前线程等待，直到闩锁倒计时到零，除非线程是 [中断][Thread.interrupt]，或指定的等待时间已过。
     *
     * 如果当前计数为零，则此方法立即返回 “true”。
     *
     * 如果计数大于零，则线程因线程调度目的而被禁用并撒谎休眠状态，直到发生以下三种情况之一：
     *
     * * 由于调用 [.countDown] 方法;或
     * * 其他线程 [interrupts][Thread.interrupt]当前线程;或
     * * 指定的等待时间已过。
     * * 如果计数达到零，则该方法返回值 'true'。
     *
     * 如果当前线程：
     * * 在进入此方法时设置了中断状态;或
     * * 在等待时是 [interrupted][Thread.interrupt]，
     *
     * 则抛出 [InterruptedException] 和当前线程的中断状态已清除。
     *
     * 如果指定的等待时间过去了，则值为“false”返回。 如果时间小于或等于零，则该方法根本不会等待。
     *
     * @param timeout 等待的最长时间
     * @param unit “timeout”参数的时间单位
     * @return 如果计数达到零，则返回 “true”和“false”
     * 如果在计数达到零之前经过的等待时间
     * @throws InterruptedException 如果等待期间当前线程中断
     */
    fun await(timeout: Long, unit: TimeUnit): Boolean {
        return latch.await(timeout, unit)
    }


}