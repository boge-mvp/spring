package com

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.common.hash.Hashing
import com.google.common.html.HtmlEscapers
import com.google.common.math.BigDecimalMath
import com.google.common.math.BigIntegerMath
import com.google.common.math.DoubleMath
import com.google.common.net.InetAddresses
import com.google.common.net.InternetDomainName
import com.google.common.net.PercentEscaper
import com.google.common.net.UrlEscapers
import com.google.common.xml.XmlEscapers
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Duration


class GuavaTest {


    /**
     * 笛卡尔乘积 测试
     */
    @Test
    fun cartesianProduct() {

        // 计算 从一组数据中取值 组合成新组合 的多种结果
        val list1 = listOf(1, 2, 3)
        val list2 = listOf(4, 5, 6)
        val list3 = listOf(7, 8)
        val list4 = listOf(9, 10)
        val list5 = listOf(11, 12)

        val cartesianProduct = Lists.cartesianProduct(list1, list2, list3, list4, list5)
        println("总共${cartesianProduct.size}个 $cartesianProduct")

    }

    @Test
    fun cacheTest() {
        val cache = CacheBuilder.newBuilder()
            //设置缓存的最大容量。
            //当缓存中的条目数量达到最大容量时，会触发缓存回收策略来清理缓存。
            //默认值-1为不限制大小。
            .maximumSize(100)
            //设置缓存条目的写入后过期时间。
            //在指定的时间段后，缓存条目将被视为过期并被清理。
            //默认值为不过期。
            .expireAfterWrite(Duration.ofDays(1))
            //设置缓存条目的访问后过期时间。
            //在指定的时间段内，如果缓存条目没有被访问，则被视为过期并被清理。
            //默认值为不过期。
            .expireAfterAccess(Duration.ofDays(1))
            //设置缓存条目的写入后自动刷新时间。
            //在指定的时间段后，缓存条目将被自动刷新，即在获取过期的缓存条目时，会异步地加载新的值。
            //默认值为不自动刷新。
            .refreshAfterWrite(Duration.ofDays(1))
            //启用弱引用作为缓存的键。
            //当缓存的键没有其他引用时，可能会被垃圾回收器回收。
            //默认值为不使用弱引用。
            .weakKeys()
            //启用弱引用作为缓存的值。
            //当缓存的值没有其他引用时，可能会被垃圾回收器回收。
            //默认值为不使用弱引用。
            .weakValues()
            //启用软引用作为缓存的值。
            //当系统内存不足时，可能会回收缓存的软引用值。
            //默认值为不使用软引用。
//            .softValues()
            //设置缓存的并发级别。
            //并发级别是指可以同时进行更新操作的线程数。
            //默认值为 4。
            .concurrencyLevel(4)
            //设置缓存的初始容量。
            //初始容量是指缓存可以容纳的初始条目数量。
            //默认值为 16。
            .initialCapacity(5)
            //设置缓存的最大权重。
            //权重是指缓存中所有条目的权重总和。
            //当缓存的权重超过最大权重时，会触发缓存回收策略来清理缓存。
            //默认值为不限制权重。
//            .maximumWeight(-1)
            //启用缓存统计信息的记录。
            //当启用缓存统计信息后，可以通过 Cache.stats() 方法获取缓存的统计数据。
            //默认值为不记录统计信息。
            .recordStats()
            //设置缓存的移除监听器。
            //当缓存中的条目被移除时，会触发移除监听器的相应操作。
            //可以自定义 RemovalListener 接口的实现来处理移除事件。
            .removalListener<String, MessageEvent> {
                println("removalListener ${it.key} - ${it.value}")
            }
            //设置缓存的权重计算器。
            //权重计算器用于计算缓存条目的权重。
            //可以自定义 Weigher 接口的实现来计算条目的权重。
//            .weigher<String, MessageEvent> { key, value ->
//                println(" weigher $key $value")
//                0
//            }
            .build(CacheLoader.from<String, MessageEvent> {
                println("cacheLoader $it")
                MessageEvent(it)
            })

        // 向缓存中放入数据
        cache.put("a", MessageEvent("A"))
        cache.put("a", MessageEvent("AA"))
        cache.put("b", MessageEvent("B"))
        println("当前缓存：${cache.size()}")

        // 从缓存中获取数据
        println("获取缓存a ${cache.getIfPresent("a")} ")
        println("获取缓存c ${cache.getIfPresent("c")} ")

        cache.invalidate("b")

        println("当前缓存：${cache.size()}")
        cache.invalidateAll()

        println("当前缓存：${cache.size()}")
    }

    /**
     * Guava 的 EventBus 是一个事件发布-订阅模型的实现，用于简化组件之间的通信。使用 Guava 的 EventBus，你可以将事件发布者（发布事件的对象）和事件订阅者（订阅事件的对象）解耦，使得它们之间的通信更加松散和灵活
     * @see @Subscribe 注解标记了事件处理方法，使得 EventBus 能够自动识别和调用这些方法。
     * @see @AllowConcurrentEvents 注解允许事件处理方法并发执行，提高了事件处理的效率。
     *
     *  EventBus 和 AsyncEventBus 是线程安全的
     *
     *
     */
    @Test
    fun eventbus() {
        // 创建EventBus实例
        val eventBus = EventBus("EventBusTest")
//        eventBus.unregister()
        // 注册订阅者
        eventBus.register(MessageSubscriber())
        // 发布事件
        eventBus.post(MessageEvent("Hello, EventBus!"))
    }

    @Test
    fun hashTest() {
        val input = "Hello, Guava!"

        // 计算字符串的 MD5 哈希值
        val md5Hash = Hashing.md5().hashBytes(input.toByteArray())
        println("MD5 Hash: $md5Hash")

        // 计算字符串的 SHA-256 哈希值
        val sha256Hash = Hashing.sha256().hashBytes(input.toByteArray())
        println("SHA-256 Hash: $sha256Hash")

        // 计算字符串的 Murmur3_128 哈希值
        val murmur3Hash = Hashing.murmur3_128().hashBytes(input.toByteArray())
        println("Murmur3_128 Hash: $murmur3Hash")

        Hashing.crc32().hashBytes(input.toByteArray())

    }

    @Test
    fun escapeTest() {
        val html = "<script>alert('Hello, Guava Escape!');</script>"

        val htmlEscaper = HtmlEscapers.htmlEscaper()
        val escapedHtml = htmlEscaper.escape(html)

        println(escapedHtml)
        // 输出: &lt;script&gt;alert(&#39;Hello, Guava Escape!&#39;);&lt;/script&gt;


        val xml = "<message>Hello, Guava Escape!</message>"

        val xmlEscaper = XmlEscapers.xmlContentEscaper()
        val escapedXml = xmlEscaper.escape(xml)

        println(escapedXml)
        // 输出: &lt;message&gt;Hello, Guava Escape!&lt;/message&gt;


        val url = "https://www.example.com/?q=Guava Escape"
        val urlEscaper = UrlEscapers.urlPathSegmentEscaper()
        var escapedUrl = urlEscaper.escape(url)
        println(escapedUrl)
        // 输出: https%3A%2F%2Fwww.example.com%2F%3Fq%3DGuava%20Escape

        // URL 编码
        val encodedUrl = UrlEscapers.urlFragmentEscaper().escape(url)
        println(encodedUrl)

        // URL 查询参数编码
        val encodedQuery: String = UrlEscapers.urlFormParameterEscaper().escape("param=value")
        println(encodedQuery)

        val percentEscaper = PercentEscaper("", false)
        escapedUrl = percentEscaper.escape(url)

        println(escapedUrl)
        // 输出: https%3A%2F%2Fwww.example.com%2F%3Fq%3DGuava%20Escape


    }


    @Test
    fun ioTest() {
        val input = "Hello, Guava!"

        // 计算字符串的 MD5 哈希值
        val md5Hash = Hashing.md5().hashBytes(input.toByteArray())
        println("MD5 Hash: $md5Hash")

        // 计算字符串的 SHA-256 哈希值
        val sha256Hash = Hashing.sha256().hashBytes(input.toByteArray())
        println("SHA-256 Hash: $sha256Hash")

        // 计算字符串的 Murmur3_128 哈希值
        val murmur3Hash = Hashing.murmur3_128().hashBytes(input.toByteArray())
        println("Murmur3_128 Hash: $murmur3Hash")

        Hashing.crc32().hashBytes(input.toByteArray())

    }

    @Test
    fun mathTest() {

//        DoubleMath 提供了一些用于双精度浮点数计算的静态方法，如舍入、取整、判断是否是整数等。
        val doubleTest = DoubleMath.roundToInt(5.57389, RoundingMode.HALF_UP) //可以将双精度浮点数四舍五入为整数。
//        BigIntegerMath 提供了一些用于大整数计算的静态方法，如加法、减法、乘法、除法、取模等。
        val bigInteger = BigIntegerMath.sqrt(BigInteger.valueOf(333), RoundingMode.CEILING) //可以计算大整数的平方根。

        val bigDecimal = BigDecimalMath.roundToDouble(BigDecimal.valueOf(5.57389) , RoundingMode.HALF_UP)

        println(doubleTest)
        println(bigInteger)
        println(bigDecimal)

    }

    @Test
    fun netTest() {

        val domain = "www.example.com"
        // 解析域名
        val parsedDomain = InternetDomainName.from(domain)
        println(parsedDomain)

        // 获取顶级私有域名
        val topPrivateDomain = parsedDomain.topPrivateDomain()
        println(topPrivateDomain)

        val ipString = "192.168.0.1"

        // 将 IP 地址字符串转换为 InetAddress 对象
        val ipAddress = InetAddresses.forString(ipString)
        println(ipAddress)

        // 将 InetAddress 对象转换为字节数组
        val ipBytes = ipAddress.address
        for (b in ipBytes) {
            print("$b ")
        }
    }

    @Test
    fun primitivesTest() {
    }

    @Test
    fun reflectTest() {
    }

    @Test
    fun concurrent() {

    }


}

// 定义一个事件类
data class MessageEvent(val message: String)

// 创建事件订阅者类
class MessageSubscriber {
    @Subscribe
    fun onMessageReceived(event: MessageEvent) {
        println("Received message: " + event.message)
    }
}