package com.apiece.coupon.domain

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponRepository : JpaRepository<Coupon, Long> {


// ** 동시성 TEST 1 **
// ** 비관적 락 **
//  READ: 읽기만 하고 수정은 안 하는데, 읽는 동안 값이 바뀌면 안 될 때 (예: 잔액 확인만 하는 리포트)
//  WRITE: 읽고 나서 그 값을 바탕으로 수정할 때 (이건 다른 트랙잭션이 락 못검 !) ← 재고 확인 후 차감이 딱 이 경우
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    fun findByIdForUpdate(@Param("id") id:Long): Coupon?        // 해당행이 없음 null로 들어가도록

    // 쿠폰 발급수량 +
    @Modifying   // JPA 수정 명시필요
    @Query("UPDATE Coupon c SET c.issuedQuantity = c.issuedQuantity + 1 WHERE c.id = :id")
    fun incrementIssuedQuantity(@Param("id") id:Long): Int

}