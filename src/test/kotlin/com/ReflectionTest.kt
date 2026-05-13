package com

import kotlin.test.Test

class ReflectionTest {




    @Test
    fun test() {


        // 创建一个字符串对象
        val str = "Hello, World!"


        // 获取字符串对象的Class对象
        val strClass= str.javaClass

        println(str)
        println(strClass)

        // 获取字符串对象的value字段
        val valueField = strClass.getDeclaredField("value")
        println(valueField)

        // 设置value字段的可访问性
        valueField.setAccessible(true)


        // 修改字符串对象的value字段的值
        val newValue = charArrayOf('H', 'E', 'L', 'L', 'O')
        valueField.set(str, newValue)


        // 输出修改后的字符串对象
        println(str)

    }

















}