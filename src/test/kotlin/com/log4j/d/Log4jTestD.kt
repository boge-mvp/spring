package com.log4j.d

import com.logger
import org.slf4j.MarkerFactory

class Log4jTestD {

    init {
        logger.trace("测试 trace D")
        logger.debug("测试 debug D")
        logger.info("测试 info D")
        logger.info(MarkerFactory.getMarker("Test"), "测试 Marker D")
        logger.warn("测试 warn D")
        logger.error("测试 error D")
    }

}