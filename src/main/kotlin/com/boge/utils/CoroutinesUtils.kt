package com.boge.utils

import com.logger
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object CoroutinesUtils {

    /**
     * 提交一个suspendable协程任务到线程池，并返回一个Deferred表示该任务的结果。
     * 当协程任务在IO调度器上执行完毕后，同样会调用completionLatch.countDown()。
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun <T, R> runGlobalAsync(
        tasks: List<T>,
        context: CoroutineContext,
        taskFun: suspend CoroutineScope.(value: T) -> R?,
        allTaskComplete: (() -> Unit)? = null
    ): List<Deferred<R?>> {
        val deferredResults = tasks.map { task ->
            GlobalScope.async(context) {
                // 尝试执行任务并捕获任何异常
                val result = runCatching {
                    taskFun.invoke(this, task)
                }
                // 如果执行失败，记录错误信息
                if (result.isFailure) {
                    logger.error("有错误:", result.exceptionOrNull())
                }
                // 返回执行结果，可能为 null
                result.getOrNull()
            }
        }
        GlobalScope.launch {
            deferredResults.forEach { it.await() } // 等待所有任务完成
            allTaskComplete?.invoke()
        }
        return deferredResults
    }

    // Dispatchers.IO
    /**
     * 在指定的协程上下文中异步执行一系列任务。
     *
     * @param tasks 要执行的任务列表。
     * @param context 协程执行的上下文环境。 Dispatchers.IO
     * @param taskFun 一个 suspend 函数，接收一个任务参数，并返回结果。
     * @param allTaskComplete 全部执行完成回调
     * @return 返回一个包含所有任务的 Deferred 对象列表，这些对象可用于异步获取任务结果。
     */
    fun <T, R> runAsync(
        tasks: List<T>,
        context: CoroutineContext,
        taskFun: suspend CoroutineScope.(value: T) -> R?,
        allTaskComplete: (() -> Unit)? = null
    ): List<Deferred<R?>> {
        val coroutineScope = CoroutineScope(context) // 创建一个基于提供的协程上下文的协程作用域
        val deferredResults = tasks.map { task ->
            coroutineScope.async {
                // 尝试执行任务并捕获任何异常
                val result = runCatching {
                    taskFun.invoke(this, task)
                }
                // 如果执行失败，记录错误信息
                if (result.isFailure) {
                    logger.error("有错误:", result.exceptionOrNull())
                }
                // 返回执行结果，可能为 null
                result.getOrNull()
            }
        }
        coroutineScope.launch {
            deferredResults.forEach { it.await() } // 等待所有任务完成
            allTaskComplete?.invoke()
        }
        return deferredResults
    }

    /**
     * 在指定的协程上下文中同步执行一系列任务。
     *
     * @param tasks 要执行的任务列表。
     * @param context 协程执行的上下文环境。 Dispatchers.IO
     * @param taskFun 一个 suspend 函数，接收一个任务参数，并返回结果。
     * @return 返回一个包含所有任务的 Deferred 对象列表，这些对象可用于异步获取任务结果。
     */
    fun <T, R> runSync(
        tasks: List<T>,
        context: CoroutineContext,
        taskFun: suspend (value: T) -> R?
    ): List<Deferred<R?>> {
        return runBlocking {
            tasks.map { task ->
                // 在指定的协程上下文中创建一个异步任务
                async(context) {
                    // 尝试执行任务并捕获任何异常
                    val result = runCatching {
                        taskFun.invoke(task)
                    }
                    // 如果执行失败，记录错误信息
                    if (result.isFailure) {
                        logger.error("有错误:", result.exceptionOrNull())
                    }
                    // 返回执行结果，可能为 null
                    result.getOrNull()
                }
            }
        }
    }


}