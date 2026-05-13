package com.boge.entity

import okhttp3.Request
import okhttp3.Response
import java.time.Duration

/**
 * 定义一个带有标识符的数据类
 */
data class RequestResult<T, D>(
    /**
     * 当前的通知状态
     *
     * 0.初始化状态 1.正常返回 2.错误 3.结束
     */
    var state : Int = 0,
    /**
     * 请求结果
     */
    var result: T? = null,
    /**
     * 错误
     */
    var error: Throwable? = null,
    /**
     * 附加数据
     */
    var data: D? = null,
    /**
     * 请求对象
     */
    var request: Request? = null

)

data class ResultData(val response: Response, val request: Request)

data class RedisLock<T>(
    /**
     * 锁定时间，默认为30秒
     */
    val lockTime: Duration = Duration.ofSeconds(30),
    /**
     * 读取数据库的函数，默认为null
     */
    val readDatabase: (() -> T?)? = null,
    /**
     * 读取数据库的函数，默认为null
     */
    val read: ((key: RedisLock<T>) -> T?)? = null,
    /**
     * 读取数据库的函数，默认为null
     */
    val write: ((key: RedisLock<T>) -> Unit)? = null,
    /**
     * 缓存时间，默认永久
     */
    val cacheTime: Duration = Duration.ZERO,
    /**
     * 锁的最大等待时间，默认为1分钟
     */
    val maxWaitTime: Duration = Duration.ofMinutes(1)
)
