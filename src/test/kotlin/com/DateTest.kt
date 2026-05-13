package com

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.Test

class DateTest {

    @Test
    fun dateFormatTest() {

        val formatter = DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.DEFAULT, Locale.CHINA
        )

        println(
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        println(
            Date().toLocalDateTime()
                .format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                )
        )
        println(
            DateFormat.getDateTimeInstance().format(Date())
        )
        println(
            formatter.format(Date())
        )

    }

}