package com.boge.serialization

import com.logger
import com.ok
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimestampSerializer : KSerializer<Timestamp> {

    override val descriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        val timestamp = value.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        encoder.encodeString(timestamp)
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        val decoderStr = decoder.decodeString()
        val timestamp = decoderStr.let { it.isBlank().ok { "1970-01-01 00:00:00" } ?: it }
        return runCatching {
            val date = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            Timestamp.valueOf(date)
        }.onFailure {
            logger.error("Timestamp 解析序列化错误 原数据:$decoderStr 验证后:$timestamp", it)
        }.getOrNull() ?: Timestamp(0)

    }

}