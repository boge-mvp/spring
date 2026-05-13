package com

import com.boge.IRequestData
import com.boge.utils.HttpUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import jakarta.annotation.Resource
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class HttpTest : BaseTest() {

    @Resource
    private lateinit var okHttpClient: OkHttpClient

    @Test
    fun test() {

        Thread.UncaughtExceptionHandler { t, e ->
            println("UncaughtExceptionHandler - ${t.name}  ${e.message}")
        }

        val urls = listOf(
            "https://baike.baidu.com/tashuo/api/getrectashuolist?lemmaId=61831787",
            "a.js",
            "https://baike.baidu.com/wikiui/api/contributor?lemmaId=61831787",
            "b.js",
            "https://baike.baidu.com/api/wikiui/sharecounter?lemmaId=61831787&method=get"
        )

        val urlsD = urls.mapIndexed { index, it ->
            IRequestData.with("$index ---------- " + it.substringAfterLast("/"), it)
        }

        val httpNet = HttpUtil
            .batchGet<Response<String>, String>(IHttp::getResult, urlsD)
            .async()
//            .httpScheduler(Schedulers.computation())
            .retry(3)

        httpNet.subscribe({
            log.info("完成进度：${httpNet.total - httpNet.count + 1} / ${httpNet.total}")
            if (it.result != null) {
                log.info("Received response: ${it.data} ${it.result?.body()}")
            } else if (it.error != null) {
                val err = it.error
                if (err is HttpException) {
                    log.error("http error=${it.data} ${err.response()?.raw()?.request?.url}", err)
                } else {
                    log.error("error", err)
                }
            }
        }, {
            log.info("All requests completed")
        })
        httpNet.await()
        log.info("Continue to execute the following tasks...")

    }

    @Test
    fun runHttpTest() {

        Thread.UncaughtExceptionHandler { t, e ->
            println("UncaughtExceptionHandler - ${t.name}  ${e.message}")
        }

        val urls = listOf(
            "https://baike.baidu.com/tashuo/api/getrectashuolist?lemmaId=61831787"
        )

        val urlsD = urls.mapIndexed { index, it ->
            IRequestData.with("$index ---------- " + it.substringAfterLast("/"), it)
        }

        val httpNet = HttpUtil
            .batchGet<Response<String>, String>(IHttp::getResult, urlsD)
//            .async()
            .httpScheduler(Schedulers.trampoline())
            .retry(3)

        httpNet.subscribe({
            log.info("完成进度：${httpNet.total - httpNet.count + 1} / ${httpNet.total}")
            if (it.result != null) {
                log.info("Received response: ${it.data} ${it.result?.body()}")
            } else if (it.error != null) {
                val err = it.error
                if (err is HttpException) {
                    log.error("http error=${it.data} ${err.response()?.raw()?.request?.url}", err)
                } else {
                    log.error("error", err)
                }
            }
        }, {
            log.info("All requests completed ${Thread.currentThread().isVirtual}")
        })
        httpNet.await()
        log.info("Continue to execute the following tasks...")

    }


    @Test
    fun runBodyTest() {

        Thread.UncaughtExceptionHandler { t, e ->
            println("UncaughtExceptionHandler - ${t.name}  ${e.message}")
        }

        val urls = listOf(
            arrayOf("https://news.baidu.com/widget", "LocalNews", "json", System.currentTimeMillis()),
//           arrayOf("https://news.baidu.com/widget", "LocalNews", "json"),
        )

        val urlsD = urls.mapIndexed { index, it ->
            IRequestData.with(it, "$index ---------- ${it.size}")
        }

//        ?ajax=json&id=ad

        val httpNet = HttpUtil
            .batchGet<Response<com.entity.Response>, String>(IHttp::localNews, urlsD)
            .async()
            .httpScheduler(Schedulers.computation())
            .retry(3)

        httpNet.subscribe({
            log.info("完成进度：${httpNet.total - httpNet.count + 1} / ${httpNet.total}")
            if (it.result != null) {
                log.info("Received response: ${it.data} ${it.result?.body()}")
            } else if (it.error != null) {
                val err = it.error
                if (err is HttpException) {
                    log.error("http error=${it.data} ${err.response()?.raw()?.request?.url}", err)
                } else {
                    log.error("error", err)
                }
            }
        }, {
            log.info("All requests completed")
        })
        httpNet.await()
        log.info("Continue to execute the following tasks...")

    }

    @Test
    fun httpTest() {

        println("1")
        val result = okHttpClient.newCall(
            Request.Builder().url("https://baike.baidu.com/tashuo/api/getrectashuolist?lemmaId=61831787").build()
        ).execute()

        println(result.body.string())
        println("2")

        TimeUnit.SECONDS.sleep(5)

    }

    @Test
    fun suspendTest() {

        println("1")

        CoroutineScope(Dispatchers.IO).launch {


            val result = HttpUtil.retrofit.create(IHttp::class.java)
                .getTest("https://baike.baidu.com/tashuo/api/getrectashuolist?lemmaId=61831787")


            logger.info(result.body())

            delay(1000)
            println("3")
        }
        println("2")

        TimeUnit.SECONDS.sleep(5)

    }

}

interface IHttp {
    @GET
    fun getResult(@Url url: String): Observable<Response<String>>

    @GET
    fun localNews(
        @Url url: String,
        @Query("id") id: String,
        @Query("ajax") ajax: String,
        @Query("r") r: Long? = null
    ): Observable<Response<com.entity.Response>>

    @GET
    suspend fun getTest(@Url url: String): Response<String>

}

