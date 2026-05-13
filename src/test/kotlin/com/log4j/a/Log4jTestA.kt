package com.log4j.a

import com.logger
import org.slf4j.MarkerFactory

class Log4jTestA {

    init {
        logger.trace("测试 trace A")
        logger.debug("测试 debug A")
        logger.info("测试 info A")
        logger.info(MarkerFactory.getMarker("Test"), "测试 Marker A")
        logger.warn("测试 warn A")
        logger.error("测试 error A")
    }

}