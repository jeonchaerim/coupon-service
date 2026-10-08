package com.apiece.coupon.api

import com.apiece.coupon.api.dto.IssuanceResponse
import com.apiece.coupon.application.IssuanceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me/issuances")
class UserIssuanceController(
    private val issuanceService: IssuanceService,
) {
    @GetMapping
    fun listMine(@RequestHeader("X-User-Id") userId: Long): List<IssuanceResponse> =
        issuanceService.findByUser(userId).map(IssuanceResponse::from)
        // IssuanceResponse내의 from 함수 사용 (람다식,지금 호출하지 말고 넘김)
}