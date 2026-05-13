package com

import org.springframework.boot.test.context.SpringBootTest
import com.boge.utils.SystemUtils
import kotlin.io.path.pathString

@SpringBootTest(classes = [MainTest::class])
class BaseTest {
    companion object {
        init {

            System.setProperty("file_prefix", "junit_")
            System.setProperty("rootDir", SystemUtils.runPath.pathString)

        }
    }


}


