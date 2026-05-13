package com

import com.boge.extend.optIntOrNull
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.sql.Timestamp
import java.time.Instant
import kotlin.test.Test

class SerializationTest {

    @Serializable
    data class testU(val name: String, @Contextual val date: Timestamp? = null)

    @Test
    fun test() {

        val json = JSON.encodeToString(testU("我来了0", Timestamp.from(Instant.now())))
        val json1 = JSON.encodeToString(testU("我来了1"))
        println(json)
        println(json1)
        println( JSON.decodeFromString<testU>("{\"name\":\"我来了0\",\"date\":\"\"}"))

    }

    @Test
    fun jsonTest() {
        val content = "{\"id\":null}"
        println(content)

        val jsonObject = Json.parseToJsonElement(content).jsonObject
        println(jsonObject.optIntOrNull("id"))


    }


}