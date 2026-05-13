package com.log4j.c

import com.logger
import org.slf4j.MarkerFactory

class Log4jTestC {

    init {
        logger.trace("测试 trace C")
        logger.debug("测试 debug C")
        logger.info("测试 info C")
        logger.info(MarkerFactory.getMarker("Test"), "测试 Marker C")
        logger.warn("测试 warn C")
        logger.error("测试 error C")
    }

}