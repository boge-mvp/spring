package com.boge.converter

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class ResultConverterFactory : Converter.Factory() {

    companion object {

        fun create(): ResultConverterFactory {
            return ResultConverterFactory()
        }

    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val originalConverter = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return ResultResponseBodyConverter(originalConverter)
    }

}


class ResultResponseBodyConverter(private val originalConverter: Converter<ResponseBody, Any>) : Converter<ResponseBody, Any> {
    override fun convert(value: ResponseBody): Any? {

        println(originalConverter)



        // 先调用原始的 Converter 进行转换
        val originalResult = originalConverter.convert(value)

        // 在这里进行自定义的二次解析
//        val customResult = parseCustomData(originalResult)

        println(" ------ $originalResult")

        return originalResult
    }

}
