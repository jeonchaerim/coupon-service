package com.apiece.coupon.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.boot.models.annotations.spi.ColumnDetails
import org.springframework.aot.generate.GeneratedTypeReference
import java.time.LocalDateTime

@Entity
@Table(
    name = "issuance",
    uniqueConstraints =  [
        UniqueConstraint(
            name = "uk_issuance_user_coupon",
            columnNames = ["user_id", "coupon_id"]
        ),
    ],
    indexes = [
        Index(name = "idx_issuance_status", columnList = "status"),
        Index(name = "idx_issuance_coupon", columnList = "coupon_id"),
    ],
)
class Issuance (
    @Column(nullable = false)
    var userId: Long,

    @Column(nullable = false)
    var couponId: Long,

    @Enumerated(EnumType.STRING)  // 열거 기본어노테이션
    @Column(length = 16)
    var status: IssuanceStatus = IssuanceStatus.ISSUED,

    @Column(nullable = false, updatable = false)
    var issuedAt: LocalDateTime,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    var usedAt: LocalDateTime ?= null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long ?= null,
){
    fun isExpired(now: LocalDateTime): Boolean =
        !now.isBefore((expiresAt))   // 만료여부: 현재시간이 만료시간과 동일하거나 이후이면

    // 상태값 변경: 사용함
    fun markUsed(now: LocalDateTime) {
        status = IssuanceStatus.USED
        usedAt = now
    }
}