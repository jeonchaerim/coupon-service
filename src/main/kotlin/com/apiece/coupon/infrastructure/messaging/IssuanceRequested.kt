package com.apiece.coupon.infrastructure.messaging

import java.time.LocalDateTime

class IssuanceRequested(
    val couponId: Long,
    var userId: Long,
    var issuedAt: LocalDateTime,
    var expiresAt: LocalDateTime
) {
}