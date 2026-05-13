package com.boge.utils

import com.logger
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.internal.operators.observable.ObservableRetryPredicate
import io.reactivex.rxjava3.internal.operators.observable.ObservableSubscribeOn
import retrofit2.Call
import retrofit2.HttpException
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

object RetrofitUtils {

    /**
     * 当Throwable 是HttpException 实现的时候 获取url地址
     */
    @JvmStatic
    fun getUrl(throwable: Throwable?): String? {
        if (throwable is HttpException) {
            return throwable.response()?.raw()?.request?.url.toString()
        }
        return null
    }

    /**
     * 获取网络访问url
     * @param single 一个 Observable
     * @return 未拿到返回null
     */
    @JvmStatic
    fun getUrl(single: Observable<*>): String? {
        val call = getCall(single)
        return call?.request()?.url?.toString()
    }

    /**
     * 获取网络访问url
     * @param single 一个 Observable
     * @return 未拿到返回null
     */
    @JvmStatic
    fun getUrl(single: Single<*>): String? {
        try {
            single::class.declaredMemberProperties.map {
                if (it.returnType.classifier == Observable::class) {
                    it.isAccessible = true
                    getUrl(it.getter.call(single) as Observable<*>)
                }
            }.firstOrNull()
        } catch (e: Exception) {
            logger.error("从Single解析url", e)
        }
        return null
    }

    @JvmStatic
    fun getCall(observable: Observable<*>): Call<*>? {
        try {
            return when (observable) {
                is ObservableRetryPredicate -> {
                    getCall(observable.source() as Observable<*>)
                }
                is ObservableSubscribeOn<*> -> {
                    getCall(observable.source() as Observable<*>)
                }
//                is CallExecuteObservable<*> -> {
//                    getCall(observable.source() as Observable<*>)
//                }
                else -> observable::class.declaredMemberProperties.map {
//                println( "${it.returnType} ${it.typeParameters}" )
                        it.isAccessible = true
                        val field = it.getter.call(observable)
                        if (field == null) return field
                        if (it.returnType.classifier == Call::class) {
                            return field as? Call<*>
                        }
//                println("子对象 ${it.name}  $field")
                        val result = field::class.declaredMemberProperties.find { it2 ->
                            it2.returnType.classifier == Call::class
                        }?.let { p ->
                            p.isAccessible = true
                            p.getter.call(field)
                        }
//                println("最后结果 $result")
                        result as? Call<*>
                    }.firstOrNull()

            }
        } catch (e: Exception) {
            logger.error("从Observable解析Call对象", e)
        }
        return null
    }

}
