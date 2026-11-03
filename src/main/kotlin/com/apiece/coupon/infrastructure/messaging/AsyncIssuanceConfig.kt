package com.apiece.coupon.infrastructure.messaging

import java.util.concurrent.Executor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
// 빈 이름
const val ISSUANCE_TASK_EXECUTOR = "issuanceTaskExecutor"

@Configuration
@EnableAsync
class AsyncIssuanceConfig {

    @Bean(name = [ISSUANCE_TASK_EXECUTOR])
    fun issuanceTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 1
        executor.queueCapacity = 10_000
        executor.setThreadNamePrefix("issuance-async-")
        executor.setWaitForTasksToCompleteOnShutdown(true) // 서버종료되옸을때 스레드풀에 있는건 다 실행된 이후 종료되도록
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        return executor
    }
}