package com.apiece.coupon.api.dto

import java.time.LocalDateTime
import javax.print.attribute.standard.PrintQuality

class CreateCouponRequest (
    val name: String,
    val totalQuality: Int = 5000,
    val validityDays: Int = 7,
    val startsAt: LocalDateTime?= null,
)