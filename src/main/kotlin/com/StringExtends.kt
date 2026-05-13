package com

fun String?.remove(remove: String, ignoreCase: Boolean = false): String? {
    if (this.isNullOrEmpty() || remove.isEmpty()) {
        return this
    }
    // 遍历给定字符串数组，检查当前字符串是否以任意一个字符串结尾
    return this.replace(remove, "", ignoreCase)
}

/**
 * 判断当前字符串是否以任何给定字符串结尾。
 *
 * @param searchStrings 一个或多个用于搜索的字符串。
 * @return 如果当前字符串以任何一个给定字符串结尾，则返回true；否则返回false。
 */
fun CharSequence?.endsWithAny(vararg searchStrings: CharSequence): Boolean {
    if (this.isNullOrEmpty() || searchStrings.isEmpty()) {
        return false
    }
    // 遍历给定字符串数组，检查当前字符串是否以任意一个字符串结尾
    return searchStrings.any {
        this.endsWith(it)
    }
}

/**
 * 判断当前字符串是否忽略大小写以任何给定字符串结尾。
 *
 * @param suffix 一个或多个用于搜索的后缀字符串。
 * @return 如果当前字符串忽略大小写以任何一个给定后缀字符串结尾，则返回true；否则返回false。
 */
fun CharSequence?.endsWithIgnoreCase(vararg suffix: CharSequence): Boolean {
    if (this.isNullOrEmpty() || suffix.isEmpty()) return false
    // 调用endsWithAny函数，但传入的参数为忽略大小写的后缀字符串数组
    return this.endsWithAny(*suffix)
}

/**
 * 检查当前字符序列是否与给定的任意一个字符序列相等。
 *
 * @param searchStrings 一个可变参数，包含要搜索的字符序列。
 * @return 如果当前字符序列与任意一个给定的字符序列相等，则返回true；否则返回false。
 */
fun CharSequence?.equalsAny(vararg searchStrings: CharSequence?): Boolean {
    if (searchStrings.isNotEmpty()) {
        // 遍历给定的字符序列，检查是否有与当前字符序列相等的
        return searchStrings.any {
            this.contentEquals(it)
        }
    }
    return false
}

/**
 * 检查当前字符序列是否与给定的任意一个字符序列在忽略大小写的情况下相等。
 *
 * @param searchStrings 一个可变参数，包含要搜索的字符序列。
 * @return 如果当前字符序列与任意一个给定的字符序列在忽略大小写的情况下相等，则返回true；否则返回false。
 */
fun CharSequence?.equalsAnyIgnoreCase(vararg searchStrings: CharSequence?): Boolean {
    if (searchStrings.isNotEmpty()) {
        // 遍历给定的字符序列，检查是否有与当前字符序列在忽略大小写的情况下相等的
        return searchStrings.any {
            this.contentEquals(it, true)
        }
    }
    return false
}

/**
 * 从当前 CharSequence 中提取位于指定标签之间的子字符串。
 * 如果标签相同，则调用 substringBetween(tag, tag) 方法。
 *
 * @param tag 标签字符串，表示要查找的起始和结束标记。
 * @return 位于两个相同标签之间的子字符串，如果找不到或输入为 null，则返回 null。
 */
fun CharSequence?.substringBetween(tag: String?): String? {
    return this.substringBetween(tag, tag)
}

/**
 * 从当前 CharSequence 中提取位于指定起始和结束标签之间的子字符串。
 *
 * @param open 起始标签字符串。
 * @param close 结束标签字符串。
 * @return 位于起始和结束标签之间的子字符串，如果找不到或任一输入为 null，则返回 null。
 */
fun CharSequence?.substringBetween(open: String?, close: String?): String? {
    if (this == null || open == null || close == null) return null // 如果输入任一为 null，直接返回 null
    val start = this.indexOf(open) // 查找起始标签的位置
    if (start != -1) { // 如果找到起始标签
        val end = this.indexOf(close, start + open.length) // 查找结束标签的位置，从起始标签后开始找
        if (end != -1) { // 如果找到结束标签
            return this.substring(start + open.length, end) // 返回位于两个标签之间的子字符串
        }
    }
    return null // 如果未找到任一标签，返回 null
}

/**
 * 从指定的字符序列中提取介于两个给定字符串之间的所有子字符串。
 *
 * @param open 开始标记字符串，用于标识子字符串的开始。
 * @param close 结束标记字符串，用于标识子字符串的结束。
 * @return 一个包含所有介于开始和结束标记之间的子字符串的数组；如果不存在这样的子字符串，或任一参数为null，则返回null。
 */
fun CharSequence?.substringsBetween(open: String?, close: String?): Array<String>? {
    // 如果输入的字符序列或标记字符串为null，直接返回null
    if (this == null || open == null || close == null) return null

    // 处理空字符序列的情况
    val strLen = this.length
    if (strLen == 0) {
        return arrayOf()
    } else {
        // 初始化标记字符串长度
        val closeLen = close.length
        val openLen = open.length

        // 用于存储提取的子字符串
        val list = mutableListOf<String>()
        var end: Int
        var pos = 0

        // 遍历字符序列，寻找并提取所有介于标记字符串之间的子字符串
        while (pos < strLen - closeLen) {
            // 寻找下一个开始标记的位置
            var start = this.indexOf(open, pos)
            if (start < 0) {
                break
            }
            // 标记开始位置后移，准备提取子字符串
            start += openLen
            // 寻找对应的结束标记的位置
            end = this.indexOf(close, start)
            if (end < 0) {
                break
            }
            // 将提取到的子字符串添加到列表中
            list.add(this.substring(start, end))
            // 更新位置到结束标记之后，准备下一次循环
            pos = end + closeLen
        }
        // 如果提取到了子字符串，则转换为数组返回，否则返回null
        return if (list.isEmpty()) null else list.toTypedArray()
    }
}

/**
 * 判断字符序列是否为空，如果为空则执行提供的lambda表达式并返回其结果。
 *
 * @param block 一个lambda表达式，当字符序列为空时被执行并返回其结果。
 * @return 如果字符序列为空，则返回执行lambda表达式的结果；如果字符序列不为空，则返回null。
 */
inline fun <T> CharSequence.isEmpty(block: () -> T): T? {
    if (this.isEmpty()) return block()
    return null
}

@Suppress("UNCHECKED_CAST")
inline fun <R> CharSequence.ifNotBlank(defaultValue: () -> R): R =
    if (isNotBlank()) defaultValue() else this as R

@Suppress("UNCHECKED_CAST")
inline fun <R> CharSequence.ifNotEmpty(defaultValue: () -> R): R =
    if (isNotEmpty()) defaultValue() else this as R

fun CharSequence.isAllNumeric(): Boolean {
    return this.matches("[0-9]+".toRegex())
}

/**
 * 从字符序列中提取所有的数字（包括整数和小数），并以字符串列表的形式返回。
 *
 * @return 返回一个包含所有提取到的数字（字符串形式）的列表。
 */
fun CharSequence.getExtractToNumber(): List<String> {
    // 使用正则表达式匹配字符序列中的数字
    val regex = "-?\\d+(\\.\\d+)?".toRegex()
    val foundNumbers = mutableListOf<String>()
    var matchResult = regex.find(this)
    while (matchResult != null) {
        foundNumbers.add(matchResult.value)
        matchResult = regex.find(this,
            matchResult.range.last + 1)
    }
    return foundNumbers
}

/**
 * 从字符序列中提取所有的整数，并以整数列表的形式返回。
 *
 * @return 返回一个包含所有提取到的整数的列表。
 */
fun CharSequence.getExtractToInt(): List<Int> {
    // 首先提取所有的数字（字符串形式），然后转换为整数形式
    val foundNumbers = this.getExtractToNumber()
    val numbersAsInt = foundNumbers.filter { it.toIntOrNull() != null }.map { it.toInt() }
    return numbersAsInt
}

/**
 * 从字符序列中提取所有的浮点数，并以浮点数列表的形式返回。
 *
 * @return 返回一个包含所有提取到的浮点数的列表。
 */
fun CharSequence.getExtractToDouble(): List<Double> {
    // 首先提取所有的数字（字符串形式），然后转换为浮点数形式
    val foundNumbers = this.getExtractToNumber()
    val numbersAsDoubles = foundNumbers.filter { it.toDoubleOrNull() != null }.map { it.toDouble() }
    return numbersAsDoubles
}
