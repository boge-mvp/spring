package com.boge.configurations

import com.JSON
import com.MarkerHttp
import com.boge.entity.ConfigYaml
import com.google.protobuf.ExtensionRegistryLite
import com.log
import jakarta.annotation.Resource
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

@Configuration
class OkHttpConfig {

    companion object {
        /**
         * 定义一个可变列表，用于存储在所有拦截器之前执行的OkHttp拦截器
         *
         * 这些拦截器接收一个Request对象，并返回一个经过处理的Request对象
         */
        val okHttpInterceptorsBefore = mutableListOf<(chain: Request) -> Request>()

        /**
         * 定义一个可变列表，用于存储在所有拦截器之后执行的OkHttp拦截器
         *
         * 这些拦截器接收一个Request对象和一个Response对象，并返回一个经过处理的Response对象
         */
        val okHttpInterceptorsLast = mutableListOf<(request: Request, response: Response) -> Response>()

    }

    @Resource
    lateinit var configYaml: ConfigYaml


    /**
     * 目前，此池最多可容纳 5 个空闲连接，这些连接将在 5 分钟不活动后被逐出
     */
    @Bean
    @ConditionalOnMissingBean
    fun httpConnectionPool(): ConnectionPool {
        return ConnectionPool(configYaml.http.pool.maxIdle, configYaml.http.pool.keepAliveDuration, TimeUnit.MINUTES)
    }

    @Bean
    fun okHttpClient(): OkHttpClient {
        log.debug("okHttpClient init")
        addOKHttpInterceptor()
        val builder =
            OkHttpClient.Builder()
                .addInterceptor { // 应用拦截器通常用于对请求或响应进行转换、缓存、重试等操作
                    var request = it.request()
                    okHttpInterceptorsBefore.forEach { v ->
                        request = v.invoke(request)
                    }
                    var response = it.proceed(request)
                    okHttpInterceptorsLast.forEach { v ->
                        response = v.invoke(request, response)
                    }
                    response
                }
                .retryOnConnectionFailure(configYaml.http.retryOnConnectionFailure)
                .connectionPool(httpConnectionPool()) //连接池
                .connectTimeout(configYaml.http.connectTimeout, TimeUnit.SECONDS) // 秒
                .writeTimeout(configYaml.http.connectTimeout, TimeUnit.SECONDS) // 秒
                .readTimeout(configYaml.http.readTimeout, TimeUnit.SECONDS) // 秒
        return builder.build()
    }

    private fun addOKHttpInterceptor() {
        okHttpInterceptorsBefore.add {
            if (configYaml.log.debug) log.info(MarkerHttp.HTTP, "url=" + it.url.toString())
            it
        }

        okHttpInterceptorsLast.add { request, response ->
            if (configYaml.log.debug && response.isSuccessful) {
                val body = request.body
                if (body != null) {
                    val buffer = Buffer()
                    body.writeTo(buffer)
                    log.debug(
                        MarkerHttp.HTTP,
                        "${body.contentType()} ${request.method} Body: " + buffer.readUtf8()
                    )
                }
                // 获取响应体的副本
                val responseBody = response.peekBody(Long.MAX_VALUE)
                if (responseBody.contentType()?.subtype in arrayOf("json", "xml", "html", "plain")) {
                    log.debug(
                        MarkerHttp.HTTP,
                        "${responseBody.contentType()} ${request.method} Body: " + responseBody.string()
                    )
                }
            }
            response
        }
    }

    @Bean
    fun retrofit(): Retrofit {
        log.info("retrofit init: baseUrl=${configYaml.http.apiBaseUrl}")
        return Retrofit.Builder()
            .client(okHttpClient())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
//            .addConverterFactory(ResultConverterFactory.create())
            .addConverterFactory(ProtoConverterFactory.createWithRegistry(ExtensionRegistryLite.newInstance()))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(JSON.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .baseUrl(configYaml.http.apiBaseUrl)
            .build()
    }
}
