package com.apiece.coupon.application

import com.apiece.coupon.support.AlreadyIssuedException
import com.apiece.coupon.support.SoldOutException
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
// 발급 가능 여부 판정을 DB 대신 Redis가 하도록 하는 부품 (Redis 판정 한 가지만 함, Service가 가져다 씀)
class CouponIssuer(
    private val redisTemplate: StringRedisTemplate,
) {

    // issue.lua 파일을 해당 경로에서 미리 읽어 둔 객체
    // 이 결과값을 가지고 1L,0L..등등 재고 판정 함
    private val script: RedisScript<Long> = RedisScript.of(
        ClassPathResource("lua/issue.lua"),
        Long::class.java,
    )

    // 최초 쿠폰 만든 이후에, 사용자에게 발행시에는 해당 함수 실행
    // 사용자 중복 검증 + 재고 검증 + 재고 차감 + 사용자 등록을 Lua 한 덩어리로 atomic 실행.
    // 실패 시 도메인 예외를 던진다.
    fun tryIssue(couponId: Long) {
        // Lua 스크립트 실행: 쿠폰 id로 만든 키 2개(재고, 명단) + 사용자 id를 넘김
        // 만약 값이 null 이면 error 던짐 null이라고
        val raw = redisTemplate.execute(
            script,
            listOf(stockKey(couponId), usersKey(couponId)),
//            userId.toString(),
        ) ?: error("Lua 스크립트 결과가 null")

        // 스크립트 판정 결과
        // 성공, 완전 매진, 이미 발행된 사람임, 이외 순으로 처리
        when (raw) {
            1L -> Unit
            0L -> throw SoldOutException()
            -1L -> throw AlreadyIssuedException()
            else -> error("예상치 못한 Lua 결과: $raw")
        }
    }

    // 쿠폰 만들 때 재고 적기 (총 발급수량 명시)
    // 실행 시점: 쿠폰 생성 (관리자가 5,000장짜리 쿠폰 만듦, 딱 한 번)
    fun initStock(couponId: Long, totalQuantity: Int) {
        redisTemplate.opsForValue().set(stockKey(couponId), totalQuantity.toString())  // stringtemplatet이라 문자열로 변환
//                              └── 키 ──────────┘  └── 값 ──────────────┘
//                              "coupon:1:stock"     "5000"
//                               coupon:1:stock = "5000"
    }

    // Redis 키 이름 만들기: 재고량, 받아간 사람 명단 (보내는 건 아니고 이름만 생성)
    // $couponId: 변수
    private fun stockKey(couponId: Long) = "coupon:$couponId:stock" // "coupon:1:stock"
    private fun usersKey(couponId: Long) = "coupon:$couponId:users" // "coupon:1:users"
}