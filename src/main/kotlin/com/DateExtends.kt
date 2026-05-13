package com

import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 将 Date 类型转换为 LocalDateTime 类型。
 * @return 转换后的 LocalDateTime 对象。
 */
fun Date.toLocalDateTime(): LocalDateTime = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

/**
 * 判断当前 LocalDateTime 对象与指定的 LocalDate 对象是否表示同一天。
 * @param date 指定的 LocalDate 对象。
 * @return 如果是同一天则返回 true，否则返回 false。
 */
fun LocalDateTime.isSameDay(date: LocalDate): Boolean = this.toLocalDate() == date

/**
 * 判断当前 LocalDateTime 对象与指定的 Timestamp 对象是否表示同一天。
 * 首先将 Timestamp 转换为 LocalDateTime，然后再判断。
 * @param date 指定的 Timestamp 对象。
 * @return 如果是同一天则返回 true，否则返回 false。
 */
fun LocalDateTime.isSameDay(date: Timestamp): Boolean = this.isSameDay(date.toLocalDateTime().toLocalDate())

/**
 * 判断当前 LocalDateTime 对象与另一个 LocalDateTime 对象是否表示同一天。
 * 首先将另一个 LocalDateTime 对象转换为 LocalDate，然后再判断。
 * @param date 指定的 LocalDateTime 对象。
 * @return 如果是同一天则返回 true，否则返回 false。
 */
fun LocalDateTime.isSameDay(date: LocalDateTime): Boolean = this.isSameDay(date.toLocalDate())

/**
 * 判断当前 LocalDateTime 对象与指定的 Date 对象是否表示同一天。
 * 首先将 Date 转换为 LocalDateTime，然后再转换为 LocalDate 进行判断。
 * @param date 指定的 Date 对象。
 * @return 如果是同一天则返回 true，否则返回 false。
 */
fun LocalDateTime.isSameDay(date: Date): Boolean = this.isSameDay(date.toLocalDateTime().toLocalDate())
