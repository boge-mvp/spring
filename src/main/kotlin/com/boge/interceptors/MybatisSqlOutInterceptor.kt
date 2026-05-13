package com.boge.interceptors

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor
import com.isNotNull
import com.logger
import com.toLocalDateTime
import org.apache.ibatis.executor.Executor
import org.apache.ibatis.mapping.BoundSql
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.ResultHandler
import org.apache.ibatis.session.RowBounds
import org.slf4j.MarkerFactory
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Matcher

/**
 * @see com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor
 */
class MybatisSqlOutInterceptor(markerName: String?, val tableNameInnerInterceptor: TableNameInnerInterceptor? = null) : InnerInterceptor {

    private val marker = markerName.isNotNull { MarkerFactory.getMarker(markerName) }

    override fun beforeQuery(
        executor: Executor?,
        ms: MappedStatement?,
        parameter: Any?,
        rowBounds: RowBounds?,
        resultHandler: ResultHandler<*>?,
        boundSql: BoundSql?
    ) {
        ms?.let { patchworkSql(it, parameter) }
    }

    override fun beforeUpdate(executor: Executor?, ms: MappedStatement?, parameter: Any?) {
        ms?.let { patchworkSql(it, parameter) }
    }

    /**
     *
     * @param mappedStatement
     * @param parameter 参数格式是map形式
     */
    private fun patchworkSql(mappedStatement: MappedStatement, parameter: Any?) {
        runCatching {
            // 获取xml中的一个select/update/insert/delete节点，是一条SQL语句
            val sqlId = mappedStatement.id // 获取到节点的id,即sql语句的id 如：com.mapper.PpqMatchMapper.insert
            val boundSql = mappedStatement.getBoundSql(parameter) // BoundSql就是封装myBatis最终产生的sql类
            val configuration = mappedStatement.configuration // 获取节点的配置
            val sql = showSql(configuration, boundSql) // 获取到最终的sql语句
            logger.info(marker, "sqlId=$sqlId, sql=$sql")
        }.onFailure {
            logger.error(marker, "执行拼接SQL语句时出错", it)
        }
    }


    // 如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号； 对参数是null和不是null的情况作了处理
    private fun getParameterValue(obj: Any?): String {
        val value = when (obj) {
            is String -> "'$obj'"
            is Timestamp -> "'" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(obj.toLocalDateTime()) + "'"
            is Date -> {
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                "'" + formatter.format(obj.toLocalDateTime()) + "'"
            }
            else -> obj?.toString() ?: "null"
        }
        return value
    }

    // 进行?的替换
    private fun showSql(configuration: Configuration, boundSql: BoundSql): String {
        // 获取参数
        val parameterObject = boundSql.parameterObject
        val parameterMappings = boundSql.parameterMappings
        // sql语句中多个空格都用一个空格代替
        var sql = boundSql.sql.replace("\\s+".toRegex(), " ")

        if (tableNameInnerInterceptor != null) {
            sql = tableNameInnerInterceptor.parserTableName(sql, boundSql)
        }

        if (CollectionUtils.isNotEmpty(parameterMappings) && parameterObject != null) {
            // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换
            val typeHandlerRegistry = configuration.typeHandlerRegistry
            // 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.javaClass)) {
                sql = sql.replaceFirst(
                    "\\?".toRegex(),
                    Matcher.quoteReplacement(getParameterValue(parameterObject))
                )
            } else {
                // MetaObject主要是封装了originalObject对象，提供了get和set的方法用于获取和设置originalObject的属性值,主要支持对JavaBean、Collection、Map三种类型对象的操作
                val metaObject = configuration.newMetaObject(parameterObject)
                for (parameterMapping in parameterMappings) {
                    val propertyName = parameterMapping!!.property
                    sql = if (metaObject.hasGetter(propertyName)) {
                        val obj = metaObject.getValue(propertyName)
                        sql.replaceFirst(
                            "\\?".toRegex(),
                            Matcher.quoteReplacement(getParameterValue(obj))
                        )
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 该分支是动态sql
                        val obj = boundSql.getAdditionalParameter(propertyName)
                        sql.replaceFirst(
                            "\\?".toRegex(),
                            Matcher.quoteReplacement(getParameterValue(obj))
                        )
                    } else {
                        // 打印出缺失，提醒该参数缺失并防止错位
                        sql.replaceFirst("\\?".toRegex(), "缺失")
                    }
                }
            }
        }
        return sql
    }

}