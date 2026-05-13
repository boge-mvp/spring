package com

import com.boge.utils.SystemUtils
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import kotlin.test.Test

class TestAnnotation : Application()

@SpringBootTest(classes = [TestAnnotation::class])
class AnnotationTest {

    companion object {
        init {
            System.setProperty("file_prefix", "junit_")
            System.setProperty("rootDir", SystemUtils.runPath.pathString)

        }
    }

    @Test
    fun test() {

        TimeUnit.SECONDS.sleep(2)

    }


}