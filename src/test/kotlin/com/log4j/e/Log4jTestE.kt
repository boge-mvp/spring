package com.log4j.e

import com.logger
import org.slf4j.MarkerFactory

class Log4jTestE {

    init {
        logger.trace("测试 trace E")
        logger.debug("测试 debug E")
        logger.info("测试 info E")
        logger.info(MarkerFactory.getMarker("Test"), "测试 Marker E")
        logger.warn("测试 warn E")
        logger.error("测试 error E")
    }

}