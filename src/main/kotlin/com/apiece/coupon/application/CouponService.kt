package com.apiece.coupon.application

import com.apiece.coupon.api.dto.CreateCouponRequest
import com.apiece.coupon.domain.Coupon
import com.apiece.coupon.domain.CouponRepository
import com.apiece.coupon.domain.Issuance
import com.apiece.coupon.domain.IssuanceRepository
import com.apiece.coupon.support.AlreadyIssuedException
import com.apiece.coupon.support.CouponNotFoundException
import com.apiece.coupon.support.NotStartedException
import com.apiece.coupon.support.SoldOutException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service   // SingleTon으로 만들기위함
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuanceRepository: IssuanceRepository,
    ) {

    @Transactional
    fun createCoupon(request: CreateCouponRequest): Coupon {
        val coupon = Coupon(
            name = request.name,
            totalQuantity = request.totalQuantity,
            validityDays = request.validityDays,
            startsAt = request.startsAt,
        )
        return couponRepository.save(coupon)
    }

    // 정합성
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        // 쿠폰 존재 확인
        val coupon = couponRepository.findByIdForUpdate(couponId)
            ?: throw CouponNotFoundException()      // null이면 익셉션 던짐
        //JAVA () -> new CouponNotFoundException() / 지금 실행하지 말고, 필요할 때 실행해"라는 코드 묶음(람다)

        val now = LocalDateTime.now();

        // 발급 가능한 시간 확인
        if (!coupon.isBookingOpen(now)) {
            throw NotStartedException()
        }

        // 쿠폰 발행수확인 (품절...)
        if (coupon.isSoldOut()) {
            throw SoldOutException()
        }

        // 회원, 쿠폰
        if(issuanceRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw AlreadyIssuedException()
        }

        // 정합성
        // 수량증가 + 저장을 한 트랜잭션 단위로 묶음
        coupon.issuedQuantity++;

        return issuanceRepository.save(
            Issuance(
                userId = userId,
                couponId = couponId,
                issuedAt = now,
                expiresAt = now.plusDays(coupon.validityDays.toLong()),
            )
        )
    }
}