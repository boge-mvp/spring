package com

import com.boge.utils.SystemUtils
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class UniquePIDTest {


    @Test
    fun test1() {

        SystemUtils.uniquePID()
        TimeUnit.SECONDS.sleep(60)

    }

    @Test
    fun test2() {
        SystemUtils.uniquePID()
        TimeUnit.SECONDS.sleep(60)

    }








}