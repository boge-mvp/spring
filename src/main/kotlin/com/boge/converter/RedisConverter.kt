package com.boge.converter

import org.springframework.core.convert.converter.Converter
import java.sql.Timestamp

class ByteArrayToTimestampConverter : Converter<ByteArray, Timestamp> {
    override fun convert(source: ByteArray): Timestamp {
        val str = source.decodeToString()
        return Timestamp(str.toLong())
    }
}