package com

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test

class JsonTest {



    @Test
    fun test() {


        val jsonObject = JsonObject(mapOf("key1" to JsonPrimitive("value1")))
        println(jsonObject)
        // 创建一个新的JsonObject，并合并现有的键值对和新的键值对
        val newJsonObject = jsonObject.toMutableMap()
        newJsonObject["key2"] = JsonPrimitive(123)

        // 输出新的JsonObject
        println(JsonObject(newJsonObject))




        println(jsonObject)




    }








}