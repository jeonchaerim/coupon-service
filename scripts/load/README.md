# 부하 테스트

`part-2-1-load-test` 의 k6 시나리오. 같은 시나리오를 `part-2-2-pessimistic-lock`, `part-2-3-redis-lua` 위에서도 그대로 돌려 결과를 비교한다.

## 사전 준비

- `brew install k6 jq`
- `docker compose up -d` 후 서비스가 8080 응답

## 구조

```
scripts/load/
├── reset.sh              공통: coupon/issuance 비우기
├── create_coupon.sh      공통: 쿠폰 생성 후 id 출력
└── part-2/
    ├── run.sh            과발급 시나리오 한 번에 실행 (reset → create → k6 → verify)
    ├── over_issuance.js
    ├── duplicate_issuance.js
    └── verify.sh
```

## 실행 (coupon/ 에서)

```bash
# 1) 과발급 검증 (재고만큼만 발급되어야 한다) — 한 번에
./scripts/load/part-2/run.sh

# 1) 과발급 검증 — 단계별
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/part-2/over_issuance.js
COUPON_ID=$COUPON_ID scripts/load/part-2/verify.sh

# 2) 중복발급 검증 (1인 1매만 쿠폰 발급되어야 한다)
./scripts/load/reset.sh
COUPON_ID=$(scripts/load/create_coupon.sh)
k6 run -e COUPON_ID=$COUPON_ID scripts/load/part-2/duplicate_issuance.js
COUPON_ID=$COUPON_ID scripts/load/part-2/verify.sh
```

## 결과 해석

verify.sh 가 한 표로 보여주는 컬럼:

| 컬럼              | 의미                                            |
| --------------- | --------------------------------------------- |
| issued_quantity | `coupon.issued_quantity` 컬럼 값 (카운터)           |
| total_quantity  | 재고 총량                                         |
| issuance_rows   | 실제로 발급된 `issuance` 행 수                        |
| duplicate_users | 같은 user 가 한 쿠폰을 2번 이상 받은 수                    |
| over_issuance   | `issuance_rows > total_quantity` 면 `FAIL`     |
| count_match     | `issued_quantity = issuance_rows` 면 `OK`      |

`issuance` 테이블의 `UNIQUE (user_id, coupon_id)` 가 같은 사용자가 같은 쿠폰을 2번 INSERT 하는 것을 DB 단에서 차단해요. 그래서 `duplicate_users` 는 항상 0입니다.

- **`over_issuance=FAIL`**: 사용자가 서로 다른 over_issuance 시나리오에서 발생. `isSoldOut()` 검사가 race 라 여러 스레드가 같은 재고를 보고 다 통과 → 재고를 넘는 INSERT.
- **`count_match=FAIL`**: 두 시나리오 모두에서 발생. 동시 트랜잭션의 `UPDATE coupon SET issued_quantity = ?` 가 서로 덮어쓰는 lost update 로 카운터가 실제 발급 수와 어긋남.

## 측정 순서

`part-2-1-load-test` (v0 결함 재현) → `part-2-2-pessimistic-lock` → `part-2-3-redis-lua`. 각 브랜치에서 위 실행을 반복하고 결과를 design 문서 6.3 절 표에 채움.

## TODO (내 로컬 환경 한계)

`part-2-1-load-test` 첫 실행 결과: 과발급은 재현됨 (issuance_rows 5,535 > 5,000, issued_quantity 593). 다만 부하가 계획대로 안 들어감.

- 계획 15만 건 중 실제 6,066건만 전송 (약 100 req/s), `dropped_iterations` 143,317
- 응답 avg 34s / p95 53.9s, 실패 13% (대부분 k6 기본 타임아웃 60s 추정)
- 원인 추정: 네트워크 아님 (localhost, 27 kB/s). Hikari 기본 풀 10개 + Docker VM CPU 4 / 메모리 3.8GB 를 앱·MySQL 이 나눠 씀 + k6 도 같은 노트북에서 실행

개선할 것

- [ ] 세 브랜치 비교 시 `rate` 를 로컬에서 버티는 수준(300~500)으로 낮춰 같은 조건으로 측정
- [ ] `run.sh` 에서 k6 종료 후 `sleep` 넣고 verify (서버가 남은 요청 처리 중이라 verify 두 번 조회 사이에 5,535 → 5,588 로 바뀜)
- [ ] 테스트 중 `docker stats` 로 앱/MySQL CPU 확인
- [ ] Hikari 풀 크기, 톰캣 스레드 수 튜닝은 나중에 따로
