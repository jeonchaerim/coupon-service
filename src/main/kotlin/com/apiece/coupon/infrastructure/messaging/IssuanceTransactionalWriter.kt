package com.apiece.coupon.infrastructure.messaging

import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class IssuanceTransactionalWriter (
    private val issuanceRepository: IssuanceRepository,
    private val couponRepository: CouponRepository,
){

    // 트랙잭션 writer 를 따로 뺀 구조 채택 이유)
    // 1. insert/update 를 하나의 트랜잭션으로 묶기 위함
    // 2. 트랜잭션 처리를 위해서 public안에서 호출해야 하므로(프록시 방식만 해당)
    @Transactional
    fun insertAndIncrement(event: IssuanceRequested) {
        issuanceRepository.save(
            Issuance(
                userId = event.userId,
                couponId = event.couponId,
                issuedAt = event.issuedAt,
                expiresAt = event.expiresAt
            )
        )
        couponRepository.incrementIssuedQuantity(event.couponId)
    }
}