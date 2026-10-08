package com.apiece.coupon.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "coupon")
class Coupon (
    @Column(nullable = false, length = 80)
    var name: String,

    // 총 발급수량
    var totalQuantity: Int,

    // 발행수량
    @Column(nullable = false)
    var issuedQuantity: Int = 0,

    // 유효기간
    @Column(nullable = false)
    var validityDays: Int = 7,

    var startsAt: LocalDateTime? = null, // null가능

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    fun isBookingOpen(now: LocalDateTime): Boolean =
        startsAt?.let { !now.isBefore(it) } ?:true
        // ?.let { } ?: 기본값  "null이 아니면 이걸 하고, null이면 이 값"
        // now 변수의 타입이 LocalDateTime 이고 반환값은 논리값
        // startsAt값은 ? null 일수 있고, null이면 ?.에서 멈춰서 .let뒤의 {} 람다는 실행하지 않고,
        // ?: true null 이니 true 로 반환함
        // startsAt값이 null이 아니면 .let실행해서 지금시간이 it(startsAt을 넘겨받음) 과 같거나 이후인가

    // 품절이다
    fun isSoldOut(): Boolean = issuedQuantity >= totalQuantity
}