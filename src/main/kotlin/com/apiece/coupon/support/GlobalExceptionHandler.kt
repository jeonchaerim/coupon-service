package com.apiece.coupon.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
// Rest API를 감시 + 특정 예외 발생시 여기로 진입하도록 하여 원하는 오류 응답값을 mapping 해줌
// 모든 컨트롤러에서 던진 예외들이 모두 이곳으로 모임. 공통부 처리
class GlobalExceptionHandler {

    // DomainException을 던지면 해당 메서드로 처리
    @ExceptionHandler(DomainException::class)
    fun handleDomain(
        ex: DomainException,    // 던져진 예외 객체 정보
        request: HttpServletRequest,    // 들어온 HTTP 요청 정보
    ): ResponseEntity<ProblemDetail> {  // ** ProblemDetail
        val pd = ProblemDetail.forStatusAndDetail(ex.httpStatus, ex.message ?: "")
        pd.title = humanize(ex.code)
        pd.instance = URI.create(request.requestURI)
        pd.setProperty("code", ex.code)

        return ResponseEntity.status(ex.httpStatus).body(pd)
    }

    private fun humanize(code: String): String =
        code.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.titlecase() } }
}