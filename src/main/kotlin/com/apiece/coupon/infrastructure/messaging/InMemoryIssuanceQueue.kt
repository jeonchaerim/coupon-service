package com.apiece.coupon.infrastructure.messaging

import com.apiece.coupon.api.dto.IssuanceResponse
import com.apiece.coupon.support.QueueFullException
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@Component
class InMemoryIssuanceQueue(
) {
    private val queue = LinkedBlockingQueue<IssuanceRequested>(CAPACITY)
    //FIFO
    fun enqueue(event: IssuanceRequested ) {
        if(!queue.offer(event)){    // 큐가 넣지 못하는 경우
            throw QueueFullException()
        }
    }

    // 메시지 꺼냄 큐에서
    fun poll(): IssuanceRequested? = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)

    fun size(): Int = queue.size

    companion object {
        private const val CAPACITY = 10_000
        private const val POLL_TIMEOUT_MS = 100L // 100ms
    }
}