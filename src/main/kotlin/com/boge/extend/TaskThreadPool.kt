package com.boge.extend

import java.util.concurrent.*

/**
 *
 * 创建一个自定义的线程池类TaskThreadPool，该类基于Java的ThreadPoolExecutor实现，并提供了额外的特性，如对任务完成状态的追踪和控制协程的支持。
 *
 * @param corePoolSize – 要保留在池中的线程数，即使它们处于空闲状态，除非 allowCoreThreadTimeOut 已设置
 * @param maximumPoolSize – 池中允许的最大线程数
 * @param keepAliveTime – 当线程数大于内核时，这是多余的空闲线程在终止之前等待新任务的最长时间。
 * @param unit – 参数的时间 keepAliveTime 单位
 * @param workQueue – 在执行任务之前用于保存任务的队列。此队列将仅 Runnable 保存该 execute 方法提交的任务。
 * @param threadFactory – 执行程序创建新线程时使用的工厂
 *
 * // 使用示例：
 *
 *     val taskThreadPool = TaskThreadPool()
 *     // 提交多个任务
 *     for (i in 1..10) {
 *         taskThreadPool.submit {
 *             println("Task $i is running on ${Thread.currentThread().name}")
 *         }
 *     }
 *     // 等待所有任务完成
 *     taskThreadPool.awaitCompletion()
 *     println("所有任务已完成")
 *     // 关闭线程池
 *     taskThreadPool.shutdownAndAwaitTermination()
 *
 */
class TaskThreadPool(
    /**
     * 核心线程池大小，默认值为当前系统的可用处理器数量的两倍。
     */
    private val corePoolSize: Int = Runtime.getRuntime().availableProcessors() * 2,

    /**
     * 最大线程池大小，默认与核心线程池大小相同。
     */
    private val maximumPoolSize: Int = corePoolSize,

    /**
     * 空闲线程存活时间，默认为60秒。
     */
    private val keepAliveTime: Long = 60L,

    /**
     * 时间单位，默认为秒（TimeUnit.SECONDS）。
     */
    private val unit: TimeUnit = TimeUnit.SECONDS,

    /**
     * 工作队列，默认为无界LinkedBlockingQueue。
     */
    @JvmField
    val workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue(),

    /**
     * 线程工厂，默认使用Executors.defaultThreadFactory()创建线程。
     */
    @JvmField
    val threadFactory: ThreadFactory = Executors.defaultThreadFactory()
) {

    /**
     * 使用给定参数初始化ThreadPoolExecutor实例。
     */
    @JvmField
    val executor = ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        workQueue,
        threadFactory
    )

    /**
     * CountDownLatch用于同步并追踪所有已提交任务的完成情况。
     */
    @JvmField
    val completionLatch = CountDownLatch(1)

    /**
     * 提交一个Runnable任务到线程池，并返回Future表示该任务的结果。
     * 在任务执行完毕后，内部会调用completionLatch.countDown()来减少计数器。
     */
    fun submit(runnable: Runnable): Future<*> {
        val future = executor.submit {
            try {
                runnable.run()
            } finally {
                completionLatch.countDown()
            }
        }
        return future
    }

    /**
     * 阻塞当前调用线程，直到所有已提交的任务全部完成。
     */
    fun awaitCompletion() {
        completionLatch.await()
    }

    /**
     * 正确关闭线程池，等待所有任务完成或在超时后强制停止未完成的任务。
     * 在关闭过程中，首先调用shutdown()方法，然后等待一定时间以确保所有任务完成。
     * 如果超时则调用shutdownNow()，并在再次等待后检查是否成功终止线程池。
     * 可能在此处额外调用completionLatch.countDown()，因为理论上此时所有任务应已完成。
     */
    fun shutdownAndAwaitTermination() {
        executor.shutdown()
        if (!executor.awaitTermination(60L, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            if (!executor.awaitTermination(60L, TimeUnit.SECONDS))
                println("线程池无法正常终止")
        }
        // 注意：此处调用completionLatch.countDown()通常不是必须的，因为在awaitTermination期间应当已经处理了所有任务
    }
}
