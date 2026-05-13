package com.boge.interceptors

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils
import com.baomidou.mybatisplus.core.toolkit.PluginUtils
import com.baomidou.mybatisplus.core.toolkit.TableNameParser
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor
import org.apache.ibatis.executor.Executor
import org.apache.ibatis.executor.statement.StatementHandler
import org.apache.ibatis.mapping.BoundSql
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.mapping.SqlCommandType
import org.apache.ibatis.session.ResultHandler
import org.apache.ibatis.session.RowBounds
import java.sql.Connection

/**
 * @see DynamicTableNameInnerInterceptor
 */
class TableNameInnerInterceptor(
    var hook: Runnable? = null,
    var tableNameHandler: ((String?, String?, BoundSql?) -> String?)? = null
) : InnerInterceptor {

    override fun beforeQuery(
        executor: Executor?,
        ms: MappedStatement,
        parameter: Any?,
        rowBounds: RowBounds?,
        resultHandler: ResultHandler<*>?,
        boundSql: BoundSql?
    ) {
        if (!InterceptorIgnoreHelper.willIgnoreDynamicTableName(ms.id)) {
            val mpBs = PluginUtils.mpBoundSql(boundSql)
            mpBs.sql(this.changeTable(mpBs.sql(), boundSql))
        }
    }

    override fun beforePrepare(sh: StatementHandler?, connection: Connection?, transactionTimeout: Int?) {
        val mpSh = PluginUtils.mpStatementHandler(sh)
        val ms = mpSh.mappedStatement()
        val sct = ms.sqlCommandType
        if (sct == SqlCommandType.INSERT || sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE) {
            if (InterceptorIgnoreHelper.willIgnoreDynamicTableName(ms.id)) {
                return
            }

            val mpBs = mpSh.mPBoundSql()
            mpBs.sql(this.changeTable(mpBs.sql(), sh?.boundSql))
        }
    }

    private fun changeTable(sql: String, boundSql: BoundSql?): String {
        ExceptionUtils.throwMpe(
            null == this.tableNameHandler,
            "Please implement TableNameHandler processing logic",
            *arrayOfNulls(0)
        )
        val string = parserTableName(sql, boundSql)
        hook?.run()
        return string
    }

    fun parserTableName(sql: String, boundSql: BoundSql?): String {
        val parser = TableNameParser(sql)
        val names = mutableListOf<TableNameParser.SqlToken?>()
        parser.accept { e -> names.add(e) }
        val builder = StringBuilder()
        var last = 0

        var name: TableNameParser.SqlToken
        val var6: Iterator<*> = names.iterator()
        while (var6.hasNext()) {
            name = var6.next() as TableNameParser.SqlToken
            val start = name.start
            if (start != last) {
                builder.append(sql, last, start)
                builder.append(tableNameHandler?.invoke(sql, name.value, boundSql))
            }
            last = name.end
        }
        if (last != sql.length) {
            builder.append(sql.substring(last))
        }
        return builder.toString()
    }

}