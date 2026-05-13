package com.boge.entity

import com.baomidou.mybatisplus.annotation.DbType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConfigurationProperties("config")
data class ConfigYaml(
    var name: String = "Application",
    var port: Int = 8080,
    var log: LogYaml = LogYaml(),
    var http: HttpYaml = HttpYaml(),
    var redis: RedisYaml = RedisYaml(),
    var mysql: MysqlYaml = MysqlYaml(),
    var mybatisPlus: MybatisInnerInterceptorYaml = MybatisInnerInterceptorYaml(),
)

@Component
@ConfigurationProperties("config.log")
data class LogYaml(
    /** 测试类的数据是否记录 */
    var debug: Boolean = true
)

@Component
@ConfigurationProperties("config.http")
data class HttpYaml(
    /** 基础url地址 */
    var apiBaseUrl: String = "",
    /** 线程池配置 */
    var pool: HttpPool = HttpPool(),
    /** 连接超时 秒 */
    var connectTimeout: Long = 10,
    /** 写入超时 秒 */
    var writeTimeout: Long = 10,
    /** 读取超时 秒 */
    var readTimeout: Long = 10,
    /** 失败链接默认重试 */
    var retryOnConnectionFailure: Boolean = false,
    /** 是否启动虚拟线程 */
    var virtualThread: Boolean = false
)

@Component
@ConfigurationProperties("config.http.pool")
data class HttpPool(
    /**
     * 每个地址的最大空闲连接数 默认5
     */
    var maxIdle: Int = 5,
    /**
     * 在保持活动持续时间，超过将清理 分钟 默认5
     */
    var keepAliveDuration: Long = 5
)

@Component
@ConfigurationProperties("config.redis")
data class RedisYaml(
    /**
     * 数据库编号，默认为0
     */
    var database: Int = 0,

    /**
     * Redis数据库的URL，可选
     */
    var url: String? = null,

    /**
     * Redis主机，默认为"localhost"
     */
    var host: String = "localhost",

    /**
     * Redis数据库的用户名，默认为"root"
     */
    var username: String = "root",

    /**
     * Redis数据库的密码，可选
     */
    var password: String? = null,

    /**
     * Redis数据库的端口号，默认为6379
     */
    var port: Int = 6379,

    /**
     * 连接超时时间，默认为5秒
     */
    var timeout: Duration? = Duration.ofSeconds(5),

    /**
     * 连接建立超时时间，默认为5秒
     */
    var connectTimeout: Duration? = Duration.ofSeconds(5),

    /**
     * Lettuce连接池配置，默认为空
     */
    var lettuce: Pool = Pool(),

    /**
     * key前缀
     */
    var keyPrefix: String = "",
    /**
     * 启动redis缓存
     */
    var enabledCache: Boolean = false,
    /**
     * 设置要保存时长  默认(null)是永久保存
     *```
     * 可以使用ISO-8601格式的字符串表示: PT30S
     * 这里的PT30S表示30秒的持续时间。其中，P表示持续时间的开始，T表示时间的开始，S表示秒。您可以根据需要调整持续时间的值。
     *
     * P7D
     * 这里的P7D表示7天的持续时间。其中，P表示持续时间的开始，D表示天。
     *
     *
     */
    var entryTtl: Duration? = null,

    /**
     * 自动检测过期的表名
     */
    var validationFails: MutableList<String> = mutableListOf()

)

@Component
@ConfigurationProperties("config.redis.pool")
data class Pool(
    /**
     * 连接池中的最大空闲连接数。默认值为 8。
     */
    var maxIdle: Int = 8,
    /**
     * 连接池中的最小空闲连接数。默认值为 0。
     */
    var minIdle: Int = 0,
    /**
     * 连接池允许的最大活动（已分配）连接数。默认值为 8。
     */
    var maxActive: Int = 8,
    /**
     * 当连接池耗尽时，客户端等待获取连接的最大时间。默认值为 `-1` 毫秒，表示永不超时。
     */
    var maxWait: Duration = Duration.ofMillis(-1),
    /**
     * 在空闲连接回收器线程运行期间检查连接空闲超时的间隔时间。
     * 如果设置为 `null`，则不执行空闲连接回收操作。
     * 默认值为 `null`。
     */
    var timeBetweenEvictionRuns: Duration? = null
)

@Component
@ConfigurationProperties("config.mysql")
data class MysqlYaml(
    var url: String? = null

)

@Component
@ConfigurationProperties("config.mybatis-plus")
data class MybatisInnerInterceptorYaml(
    /**
     * 定义Mybatis-Plus插件的拦截器列表，初始化为空可变列表，表示可以动态添加拦截器
     */
    var interceptor: List<String> = mutableListOf(),

    /**
     * 是否启用表名映射功能，默认为启用(true)
     */
    var enabledTableName: Boolean = true,

    /**
     * sql日志配置
     */
    var sqlLog: SqlLogYaml = SqlLogYaml(),

    /**
     * 是否启用分页功能，默认为启用(true)
     */
    var enabledPage: Boolean = true,

    /**
     * 分页数据库类型，默认为MySQL
     */
    var pageDbType: DbType = DbType.MYSQL
)

@Component
@ConfigurationProperties("config.mybatis-plus.sql-log")
data class SqlLogYaml(
    /**
     * 是否启用sql日志输出功能，默认为启用(false)
     */
    var enabledSqlLog: Boolean = false,

    /**
     * sql日志Marker标记，默认null
     */
    var sqlLogMarker: String? = null,
)
