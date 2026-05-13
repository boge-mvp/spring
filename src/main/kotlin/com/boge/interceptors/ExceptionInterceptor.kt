package com.boge.interceptors

import com.logger
import org.apache.ibatis.cache.CacheKey
import org.apache.ibatis.executor.Executor
import org.apache.ibatis.executor.statement.StatementHandler
import org.apache.ibatis.mapping.BoundSql
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.plugin.*
import org.apache.ibatis.session.ResultHandler
import org.apache.ibatis.session.RowBounds
import java.sql.Connection
import java.sql.SQLException

@Intercepts(
    Signature(type = StatementHandler::class, method = "prepare", args = [Connection::class, Int::class]),
    Signature(type = StatementHandler::class, method = "getBoundSql", args = []),
    Signature(type = Executor::class, method = "update", args = [MappedStatement::class, Any::class]),
    Signature(
        type = Executor::class,
        method = "query",
        args = [MappedStatement::class, Any::class, RowBounds::class, ResultHandler::class]
    ),
    Signature(
        type = Executor::class,
        method = "query",
        args = [MappedStatement::class, Any::class, RowBounds::class, ResultHandler::class, CacheKey::class, BoundSql::class]
    )
)
class ExceptionInterceptor : Interceptor {

    @Throws(Exception::class)
    override fun intercept(invocation: Invocation): Any? {
        try {
            return invocation.proceed()
        } catch (e: Exception) {
            when (e) {
                is SQLException -> {
                    // 处理 SQL 相关异常，如转换错误等
                    logger.error("SQL execution error", e)
                }
                else -> throw e
            }
        }
        return null
    }

    override fun plugin(target: Any): Any {
        return Plugin.wrap(target, this)
    }

}
