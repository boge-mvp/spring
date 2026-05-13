package com.boge

import org.apache.ibatis.mapping.BoundSql

/**
 * 网络请求的访问数据
 */
interface IRequestData<T, P> {

    /**
     * 参数
     */
    val param: Array<out P>

    /**
     * 附带值
     */
    val data: T?

    companion object {
        /**
         * 创建一个编辑创建
         */
        fun <T, V> with(param: Array<V>, data: T): IRequestData<T, V> {
            return object : IRequestData<T, V> {
                override val param = param
                override val data = data
            }
        }

        /**
         * 创建一个编辑创建
         */
        fun <T, V> with(data: T, vararg param: V) = with(param, data)

        /**
         * 创建一个编辑创建
         */
        fun <T, V> withParam(vararg param: V) = with(param, null)

    }

}


interface ITableName {
    /**
     * 要自定义table名字
     */
    fun onTableName(sql: String?, name: String?, boundSql: BoundSql): String? {
        return name
    }

}