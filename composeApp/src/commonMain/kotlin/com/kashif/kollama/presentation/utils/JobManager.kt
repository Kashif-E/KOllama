package com.kashif.kollama.presentation.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Node<T : Any>(
    val value: T,
    var prev: Node<T>? = null,
    var next: Node<T>? = null
)

class DoublyLinkedList<T : Any> {
    private var head: Node<T>? = null
    private var tail: Node<T>? = null
    private var size: Int = 0

    fun addToFront(value: T): Node<T> {
        val newNode = Node(value)
        if (head == null) {
            head = newNode
            tail = newNode
        } else {
            newNode.next = head
            head?.prev = newNode
            head = newNode
        }
        size++
        return newNode
    }

    fun moveToFront(node: Node<T>) {
        if (node === head) return

        // Remove from current position
        node.prev?.next = node.next
        node.next?.prev = node.prev

        if (node === tail) {
            tail = node.prev
        }

        // Move to front
        node.prev = null
        node.next = head
        head?.prev = node
        head = node
    }

    fun removeLast(): T? {
        val lastNode = tail ?: return null
        if (head === tail) {
            head = null
            tail = null
        } else {
            tail = lastNode.prev
            tail?.next = null
            lastNode.prev = null
        }
        size--
        return lastNode.value
    }

    fun remove(node: Node<T>) {
        if (node === head) head = node.next
        if (node === tail) tail = node.prev

        node.prev?.next = node.next
        node.next?.prev = node.prev

        node.prev = null
        node.next = null
        size--
    }

    fun clear() {
        head = null
        tail = null
        size = 0
    }

    fun size(): Int = size
}

class ChatJobManager {
    private val mutex = Mutex()
    private val jobMap: MutableMap<String, JobState> = mutableMapOf()
    private val lruList = DoublyLinkedList<String>()

    data class JobState(
        val job: Job,
        val responseBuilder: StringBuilder = StringBuilder(),
        val thinkingStack: MutableList<String> = mutableListOf(),
        var isInThinkingBlock: Boolean = false,
        var node: Node<String>? = null
    )

    companion object {
        private const val MAX_CONCURRENT_CHATS = 5
    }

    suspend fun getActiveSessionIds(): Set<String> = mutex.withLock {
        jobMap.keys.toSet()
    }
    suspend fun addJob(sessionId: String, job: Job): JobState = mutex.withLock {
        // Remove oldest entry if at capacity
        if (jobMap.size >= MAX_CONCURRENT_CHATS && !jobMap.containsKey(sessionId)) {
            lruList.removeLast()?.let { oldestId ->
                jobMap.remove(oldestId)?.let { oldState ->
                    oldState.job.cancel()
                    oldState.node = null
                }
            }
        }

        // Create new job state and add to front of LRU
        val node = lruList.addToFront(sessionId)
        val jobState = JobState(
            job = job,
            node = node
        )
        jobMap[sessionId] = jobState
        jobState
    }

    suspend fun getJobState(sessionId: String): JobState? = mutex.withLock {
        jobMap[sessionId]?.also { state ->
            state.node?.let { node ->
                lruList.moveToFront(node)
            }
        }
    }

    suspend fun removeJob(sessionId: String) = mutex.withLock {
        jobMap.remove(sessionId)?.let { state ->
            state.job.cancel()
            state.node?.let { node ->
                lruList.remove(node)
            }
        }
    }

    suspend fun cancelAllJobs() = mutex.withLock {
        jobMap.values.forEach { it.job.cancel() }
        jobMap.clear()
        lruList.clear()
    }
}
