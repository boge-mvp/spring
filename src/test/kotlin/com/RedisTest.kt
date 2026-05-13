package com

import com.boge.RedisMapper
import com.boge.entity.RedisLock
import jakarta.annotation.Resource
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.ScanOptions
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/**
 *
 *
 * [https://docs.spring.io/spring-data/redis/reference/repositories/core-concepts.html](https://docs.spring.io/spring-data/redis/reference/repositories/core-concepts.html)
 *
 */
class RedisTest : BaseTest() {

    @Resource
    private lateinit var redisMapper: RedisMapper

    val NAME = "TestName"

    @Test
    fun readTest() {

        println(JSON.encodeToString("aa"))

        val content = "{}"


        println(redisMapper.get(NAME))
        val u = redisMapper.get<User>(NAME)
        println(u)


        val a = redisMapper.getHash<String, User>("aaaa", listOf("1", "2"))

        println(a)

    }

    @Test
    fun writeTest() {
        redisMapper.add(NAME, User(null, "xx", 12))
    }

    @Test
    fun listTest() {

        val users = MutableList(20) {
            User(null, "name_$it", it * 10)
        }
//        redisMapper.add("names", users)
//        redisMapper.addList("nameList", users)
//        redisMapper.addHash("nameHash", users.mapIndexed { index, user -> "name_${index}" to user }.toMap())
//        redisMapper.addSet("nameSet", users.toSet())


//        val list = redisMapper.redis.boundListOps("names")
//        list.range(0, 1)
//
//        val uu = redisMapper.get<List<User>>("names")!!
//        println(uu)


        val scan = ScanOptions.scanOptions()
            .match("*name_3*")
            .count(10)
            .build()

        val result = redisMapper.redis.boundSetOps("nameSet").scan(scan)!!
        println(result.position)

        while (result.hasNext()) {
            println(result.next())
        }

//        val sortQuery = SortQueryBuilder
//            .sort("nameList")
//            .by("10")
//            .build()
////        QueryUtils.convertQuery()
//
//        val list = redisMapper.redis.sort(sortQuery)
//        println(list.size)
//        println(list)

    }


    @Test
    fun lockTest() {
        val name = "lockTest"
        val sleep = 5L
        (1..10).forEach {
            thread {
                val string: String? = redisMapper.lock(
                    name,
                    RedisLock(
                        cacheTime = Duration.ofSeconds(30),
                        maxWaitTime = Duration.ofSeconds(it.toLong()),
                        read = {
                            redisMapper.getList(name, 0)
                        },
                        readDatabase = {
                            logger.info("读数据")
                            TimeUnit.SECONDS.sleep(sleep)
                            "ccc"
                        },
                        write = {
                            logger.info("保存了数据")
                            redisMapper.addList(name, "ccc", it.cacheTime)
                        },
                    )
                )
                logger.info("结果:$string")

            }
        }
        TimeUnit.SECONDS.sleep(sleep + 10)
    }


    @Test
    fun lockBlockTest() {
        val name = "lockBlockTest"
        logger.info("测试开始")
        (1..3).forEach {
            thread {
                redisMapper.lockBlock(
                    name,
                    {
                        logger.info("$it aa 需要休息 ${it + 10}s")
                        TimeUnit.SECONDS.sleep(it + 10L)
                    },
                    Duration.ofSeconds(15),
                    Duration.ofSeconds(10),
                )
            }
        }
        TimeUnit.SECONDS.sleep(30)
    }


    @Test
    fun hashTest() {


//        redisMapper.addHash("testTime", "aa", "cc")
        redisMapper.addHash<String, String>(
            "testTime",
            (1..10).associate { it.toString() to "值是$it" },
            Duration.ofSeconds(10)
        )
        redisMapper.addHash("testTime", "22", "值是", Duration.ofSeconds(15))

    }

}