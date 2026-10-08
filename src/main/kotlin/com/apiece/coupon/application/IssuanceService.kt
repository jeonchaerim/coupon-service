package com.apiece.coupon.application

import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import com.apiece.coupon.domain.IssuanceStatus
import com.apiece.coupon.support.AlreadyIssuedException
import com.apiece.coupon.support.AlreadyUsedException
import com.apiece.coupon.support.ExpiredException
import com.apiece.coupon.support.IssuanceNotFoundException
import com.sun.jdi.LongValue
import org.apache.catalina.filters.ExpiresFilter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service   // SingleTon으로 만들기위함
class IssuanceService(
    private val issuanceRepository: IssuanceRepository,
    ) {

    @Transactional
    // 발급 내역 조회 + 검증 + 상태변경을 한 트랙잭션으로 묶음
    fun use(issuanceId: Long, userId: Long): Issuance {
        val issuance = issuanceRepository.findById(issuanceId)
            .orElseThrow{ IssuanceNotFoundException() }

        when(issuance.status) {
            IssuanceStatus.USED -> throw AlreadyUsedException()
            IssuanceStatus.EXPIRED -> throw ExpiredException()
            IssuanceStatus.ISSUED -> Unit
        }

        val now = LocalDateTime.now()
        // 만료시간 확인
        if (issuance.isExpired(now)) {
            throw ExpiredException()
        }

        // 상태값 변경: 사용함
        issuance.markUsed(now)
        return issuance
    }

    fun findByUser(userId: Long): List<Issuance> =
        issuanceRepository.findByUserIdOrderByIssuedAtDesc(userId)
}