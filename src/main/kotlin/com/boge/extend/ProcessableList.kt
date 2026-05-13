package com.boge.extend

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 可处理列表类，用于在元素数量达到预设最大值时触发批量处理并清除元素。
 *
 * @param len 列表的最大长度，当添加元素导致列表大小等于或超过此参数设定的长度时，
 *            将调用用户提供的[processAll]函数对所有元素进行处理，并清空列表。
 * @param processAll 用户自定义处理函数，接收一个类型为T的元素列表作为参数，在满足条件时执行处理逻辑。

 * ProcessableList 类内部使用了线程安全的数据结构 ConcurrentLinkedDeque 来存储元素。
 * 当向列表中添加元素，且列表容量达到或超过设定的最大长度时，会自动触发对当前列表中所有元素的一次性处理，
 * 并在处理完成后清空列表。这样设计确保了数据能够及时得到处理，同时维持列表长度在可控范围内。
 */
class ProcessableList<T>(
    private val len: Int,
    val processAll: (List<T>) -> Unit
) {

    private val deque = ConcurrentLinkedDeque<T>()

    /**
     * 添加一个元素到列表中。如果添加后列表长度达到或超过了预设的最大长度，
     * 将调用 [processAndClear] 方法来处理和清除列表中的元素。
     *
     * @param item 要添加到列表中的元素。
     */
    fun add(item: T) {
        deque.addLast(item)
        if (deque.size >= len) { // 当队列大小达到或超过len时
            processAndClear()
        }
    }

    /**
     * 获取当前列表中的元素数量。
     */
    val size: Int
        get() = deque.size

    /**
     * 清空列表中的所有元素。
     */
    fun clear() = deque.clear()

    /**
     * 手动激发满足最大队列后的执行操作
     */
    fun complete() {
        processAndClear()
    }

    /**
     * 私有方法，同步地将列表中的所有元素取出、处理并清空。
     * 先将所有元素收集到一个新的列表中，然后调用用户提供的[processAll]函数进行处理，
     * 最后清空原始队列中的元素。
     */
    private fun processAndClear() {
        synchronized(deque) {
            val itemsToProcess = mutableListOf<T>()
            while (!deque.isEmpty()) {
                itemsToProcess.add(deque.pollFirst())
            }
            processAll(itemsToProcess)
        }
    }

    /**
     * 将内部维护的 Deque 结构转换为标准 Kotlin List。
     */
    fun toList(): List<T> {
        return deque.toList()
    }
}

