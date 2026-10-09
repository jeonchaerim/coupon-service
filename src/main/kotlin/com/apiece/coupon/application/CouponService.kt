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
    private val couponIssuer: CouponIssuer,
    ) {

    @Transactional
    fun createCoupon(request: CreateCouponRequest): Coupon {
        // 쿠폰 생성 먼저
        val coupon = couponRepository.save(Coupon(
            name = request.name,
            totalQuantity = request.totalQuantity,
            validityDays = request.validityDays,
            startsAt = request.startsAt,
        ))
        // 재고 키, 쿠폰 id 그리고 5000개
        couponIssuer.initStock(coupon.id!!, coupon.totalQuantity)
        return coupon
    }

    // 정합성
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        // 쿠폰 존재 확인
//        val coupon = couponRepository.findByIdForUpdate(couponId)
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CouponNotFoundException() }
//            ?: throw CouponNotFoundException()      // null이면 익셉션 던짐
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

        // 정합성 3. Redis + Lua
        // 여기만 Redis 임계범위
        // 여기만 Redis 메모리 내에 저장됨
        // 원자적 연산
        // 재고 및 발급수량 늘린 부분까지 Redis 처리 후 하단에 성공건은 DB에서 수량 증가하도록 처리
        couponIssuer.tryIssue(couponId)
        // >> 아래는 분리
        couponRepository.incrementIssuedQuantity(couponId)

        /*
        // 정합성
        // 수량증가 + 저장을 한 트랜잭션 단위로 묶음
        coupon.issuedQuantity++;
         */

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