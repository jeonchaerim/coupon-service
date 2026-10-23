package com.apiece.coupon.infrastructure.messaging

import com.apiece.coupon.domain.Issuance
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.io.Writer
import kotlin.concurrent.thread

private val log = KotlinLogging.logger { } // 빈 람다 >> 이 로그가 위치하는 클래스를 찾기 위함.클래스 이름이 바뀌어도 변경할 필요 X

@Component
class InMemoryIssuanceWorker (
    private val queue: InMemoryIssuanceQueue,
    private val writer: IssuanceWriter,
){
    // lateinit 지금은 값이 없지만 나중에 넣겠다
    private lateinit var workerThread: Thread

    // @Component 빈 만들고 주입 이후 자동으로 1회 실행함, 앱이 켜질때 워커도 자동으로 켜지도록 함
    // isDaemon > 데몬 스레드는 "앱 종료를 막지 않는 보조 일꾼. 백그라운드에서 실행.
    // JVM이 종료됨과 동시에 함께 끝남. 앱이 종료되지 않는 문제는 해소해 주지만 중간에 끊길 수 있어서 인메모리 큐의 유실 문제와 연결
    @PostConstruct
    fun start() {
        workerThread = thread(name = "issuance-worker", isDaemon = true) {
            while(!Thread.currentThread().isInterrupted) {  // 방해하지 않는 이상 계속 실행
                val event = try {
                    queue.poll() ?: continue    // 비어있으면 기다림
                } catch (c: InterruptedException) {
                    Thread.currentThread().interrupt()  // 익셉션 발생하면 방해받았다고 스레드에 status 지정
                    break
                }
                try {
                    writer.write(event)
                } catch (e: Exception) {
                    log.error (e){ "Worker write 실패: couponId=${event.couponId}, userId=${event.userId}" }
                }
            }
            log.info { "issuance-worker 종료" }
        }
    }

    // 컴포넌트 빈이 메모리에 없을때 실행 요청 > 앱 종료직전 실행
    @PreDestroy
    fun stop() {
        if(::workerThread.isInitialized) { // 워커 스레드가 만들어진 상태면
            workerThread.interrupt()       // 깨워서 멈추라고 신호 보냄
        }
    }
}