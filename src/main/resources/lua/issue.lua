-- 발급 원자 처리!!!  Redis 단일 스레드 + 스크립트 단위 atomic 으로 >> lua를 쓰는 이유
-- 재고 차감 + 사용자 추가 를 한 덩어리에 묶는다.
--
-- KEYS[1] = coupon:{id}:stock   (재고 카운터, 행사 생성 시 totalQuantity 로 초기화)
-- KEYS[2] = coupon:{id}:users   (발급된 사용자 set)
-- 반환:
--   1   발급 성공
--   0   매진
--  -1   이미 발급된 사용자

local remaining = tonumber(redis.call('GET', KEYS[1]) or '0')
if remaining <= 0 then
  return 0
end
redis.call('DECR', KEYS[1])
return 1
