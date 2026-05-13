package com

import jakarta.annotation.Resource
import org.junit.jupiter.api.Test
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByNameAndAgeIn(vararg age: Array<Any>): List<User>
    fun findByName(name: String): List<User>
    fun findByAgeIn(vararg age: Long): List<User>
    fun findByAge(age: Long): List<User>
    fun findByIdIn(vararg age: Long): List<User>
    fun findByNameAndAge(name: String, age: Long): List<User>
    // 省略其他查询方法


}

/**
 *
 *
 * [https://docs.spring.io/spring-data/redis/reference/repositories/query-keywords-reference.html](https://docs.spring.io/spring-data/redis/reference/repositories/query-keywords-reference.html)
 *
 */
class RedisRepositoryTest : BaseTest() {

    @Resource
    private lateinit var userRepository: UserRepository

    @Test
    fun saveUser() {
        val users = MutableList(10) {
            User(it + 1L, "name_$it", it * 2, Timestamp.valueOf(LocalDateTime.now()))
        }
        userRepository.saveAll(users)

        val count = userRepository.count()
        println("保存的数量 $count")

        val findById = userRepository.findById(2L)
        println("查询到的数据 $findById")

        val findAllById = userRepository.findAllById(listOf(1L, 2L))
        println("查询到的数据 $findAllById")

        val exists = userRepository.existsById(3L)
        println("是否存在= $exists")

        val findAll = userRepository.findAll()
        println("查询到的数据 $findAll")

        userRepository.deleteById(5L)

        TimeUnit.SECONDS.sleep(15)
    }


    @Test
    fun findMethod() {
//        val user = userRepository.findByName("name_2")
//        println(user)

        var users = userRepository.findByNameAndAge("name_0", 0)
        println(users)

//        users = userRepository.findByNameAndAgeIn(arrayOf("name_0", 0), arrayOf("name_5", 10))
//        println(users)

        users = userRepository.findByIdIn(1, 2, 6, 9)
        println(users)

        users = userRepository.findByAgeIn(14, 18, 1000)
        println(users)

        users = userRepository.findByAge(14)
        println(users)

    }


    @Test
    fun changeUserTest() {

        val user = User(1241L, "name_test", 5, Timestamp.valueOf(LocalDateTime.now()), 60)
        userRepository.save(user)

        TimeUnit.SECONDS.sleep(10)

        user.age = 10
        userRepository.save(user)


        TimeUnit.SECONDS.sleep(15)
    }

    @Test
    fun getUsersByAge() {

        val us = userRepository.findById(2)
        println(us)

        val u = userRepository.findByAge(12)
        println(u)
    }

    @Test
    fun getUsersByName() {
        val u = userRepository.findByName("name_5")
        println(u)
    }


}