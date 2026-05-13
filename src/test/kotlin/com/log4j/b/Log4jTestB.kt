package com.log4j.b

import com.logger
import org.slf4j.MarkerFactory

class Log4jTestB {

    init {
        logger.trace("测试 trace B")
        logger.debug("测试 debug B")
        logger.info("测试 info B")
        logger.info(MarkerFactory.getMarker("Test"), "测试 Marker B")
        logger.warn("测试 warn B")
        logger.error("测试 error B")
    }

}