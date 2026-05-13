package com

import kotlin.test.Test

class StringTest {

    @Test
    fun test() {


        val str = "我来了我343.534你说呢1.20023wdw你770.1587好额"

        println(str.getExtractToNumber())

    }

}