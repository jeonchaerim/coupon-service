---
기본 설정 (프젝 세팅, 쿠폰 발급/사용 API, Jib 이미지 빌드)
---

### 한 것
- git init, part-1 브랜치에서 작업
- docker-compose로 mysql 띄움 (redis는 일단 뺌)
- Coupon / Issuance 엔티티, 쿠폰 생성 / 발급 / 사용 / 내 발급목록 API
- sealed DomainException + GlobalExceptionHandler (ProblemDetail 응답)
- Jib으로 coupon-service 이미지 빌드 → compose로 띄우고 scripts/api.sh로 확인

### 배운 것

#### git / 터미널
- git init 할 때 hint 뜨는 거 에러 아님. 기본 브랜치 master로 만든다는 안내일 뿐
  - `git branch -m main`, `git config --global init.defaultBranch main`
- git branch 쳤는데 `~` `(END)` 화면 뜬 거 → less(페이저)가 빈 문서 연 거임. q로 나가면 됨
  - 커밋 0개면 브랜치 실체가 없어서 git branch 출력이 비어있음
  - 강사님은 바로 프롬프트로 돌아오는데 나만 뜬 이유: oh-my-zsh가 `LESS=-R` 설정해서 git 기본 옵션(-F) 덮어씀
  - `git config --global core.pager "less -FRX"` → F: 한 화면이면 바로 종료 / R: 색 유지 / X: 닫아도 출력 남김

#### JPA
- `@GeneratedValue` 기본값은 IDENTITY 아니고 AUTO임
  - Hibernate 6 + MySQL에서 AUTO는 시퀀스 흉내내는 `xxx_seq` 테이블 만듦 → MySQL이면 IDENTITY 명시하는 게 일반적
  - IDENTITY는 INSERT 해야 id 알 수 있어서 배치 INSERT 안 됨
- Spring Data 메서드 이름 쿼리: `findBy` `existsBy` `countBy` `deleteBy` 철자 정확해야 함
- `@Transactional` 안에서 조회한 엔티티 필드만 바꾸면 save 안 해도 커밋 때 UPDATE 나감 (변경 감지)
- `@Enumerated(STRING)` → Hibernate 6부터 MySQL에선 VARCHAR 말고 ENUM 타입 컬럼으로 만듦
  - 그래서 `@Column(length = 16)` 의미 없음. VARCHAR로 하고 싶으면 `@JdbcTypeCode(SqlTypes.VARCHAR)`
  - ddl-auto: update라 재시작마다 alter 로그 뜰 수 있음. 에러 아님
- show-sql: true + org.hibernate.SQL: DEBUG 둘 다 켜면 SQL 로그 두 번 찍힘

#### 코틀린 문법 (자바랑 비교)
- 이름이 먼저, 타입은 뒤. `:` 나오면 "타입은" 이라고 읽으면 덜 헷갈림
  - `fun create(request: CreateCouponRequest): ResponseEntity<CouponResponse>`
  - 클래스 뒤 `:`만 상속 (`class A : RuntimeException()`)
- `fun 이름()` 괄호 = 파라미터. val/var 안 붙이고 자동으로 val처럼 동작
- `class 이름()` 괄호 = 주 생성자. 여기에 val/var 붙이면 필드까지 됨
  - 생성자 주입: `class CouponController(private val couponService: CouponService)` 한 줄로 끝 (자바 필드+생성자+대입)
  - 예외 클래스에서 message는 val 없이 부모한테 넘기기만 함 → `: RuntimeException(message)`
- 기본값 파라미터: `message: String = "쿠폰 정보를 찾을 수 없습니다"` → 안 넘기면 기본값. 자바는 생성자 두 개 만들어야 함
- `= 식` 한 줄 함수: `=` 뒤 식 결과가 리턴값. 식 하나일 때만 가능
- `val` = final, `var` = 재할당 가능, 타입 생략하면 추론. new 없음, 세미콜론 없음
- 마지막 파라미터 뒤 쉼표(trailing comma) 허용됨

#### null 처리
- `LocalDateTime?` → ?는 타입에 붙은 것. null 허용. `?=` 같은 연산자 없음 (`LocalDateTime? = null` 임)
- ? 붙였다고 꼭 null로 시작해야 하는 건 아님. null 넣을 "수도" 있게 된 거
- null 가능 → null 불가 자리에 넣으면 컴파일 에러. 반대는 됨
  - ?는 그 변수 하나에만 붙음. 엔티티 createdAt이 `?`여도 Response의 createdAt은 따로 선언한 타입대로 감
  - 해결: 받는 쪽도 ? / `requireNotNull()` / 애초에 보내는 쪽에서 ? 빼기 (now()로 항상 채워지면 ? 필요 없음)
- `?.` 안전호출: null이면 거기서 멈추고 결과 null
- `?:` 엘비스: 왼쪽이 null이면 오른쪽 값
- `startsAt?.let { !now.isBefore(it) } ?: true`
  - startsAt null이면 → true (발급 시작 제한 없음)
  - 아니면 → 지금이 startsAt 이후거나 같으면 true
  - `?.let { } ?: 기본값` = "null 아니면 이거, null이면 이 값" 패턴
- `it` = 람다에 넘어온 값 하나의 기본 이름. `{ start -> ... }` 처럼 이름 직접 붙여도 됨
- `ex.message ?: ""` → 자바에서 온 message는 코틀린에서 String?로 보임

#### 람다 / 제네릭 / 함수참조
- `.orElseThrow { CouponNotFoundException() }`
  - `()`로 쓰면 예외 객체를 미리 만들어서 넘김 → 타입 안 맞음
  - `{}`는 람다 = "없으면 그때 이렇게 만들어" 하고 실행 안 한 채로 넘김
  - 마지막 인자가 람다면 괄호 밖으로 빼고 빈 괄호 생략 가능
  - 자바 `() -> new X()` 랑 같음
- 제네릭 T = 타입 빈칸. `<>` 안에 타입 쓰면 채워짐 (함수 파라미터 x에 값 넣는 거랑 같은 원리)
  - `JpaRepository<T, ID>` 에 `<Coupon, Long>` → 순서대로 T=Coupon, ID=Long
  - 그래서 findById가 `Optional<Coupon> findById(Long id)` 가 됨
- `.map(IssuanceResponse::from)` → 리스트 원소 하나하나 from으로 변환
  - `::` 함수 참조 = `{ IssuanceResponse.from(it) }` 줄인 거
  - 자바는 stream() / toList() 필요한데 코틀린은 바로 됨
  - 엔티티 그대로 내보내지 말고 DTO로 바꿔서 응답

#### sealed
- 봉인하는 건 when이 아니라 "하위 클래스 목록"
- 같은 패키지/모듈 안에서만 상속 가능 → 컴파일러가 자식 전체 목록을 앎
- 그래서 when에서 else 없이 다 처리했는지 검사해줌. 새 예외 추가하고 처리 빠뜨리면 컴파일 에러로 알려줌
- sealed 클래스 자체는 직접 생성 불가 (abstract처럼)

#### GlobalExceptionHandler
- `@RestControllerAdvice` → 컨트롤러 밖으로 던져진 예외 다 여기로 모임
- `@ExceptionHandler(DomainException::class)` → 자식 예외 8개 전부 이걸로 잡힘
- 파라미터(ex, request) 받고 → ProblemDetail 채우고(status, detail, title, instance) → `ResponseEntity.status(...).body(pd)` 로 리턴
- pd 안의 status는 JSON 본문용, `.status()`는 진짜 HTTP 상태코드. 둘 다 맞춰야 함 (안 하면 본문엔 404인데 실제론 200)
- humanize: `COUPON_NOT_FOUND` → `Coupon Not Found`
- `problemdetails.enabled: true` 라서 스프링 기본 404도 같은 형식으로 나옴

#### Jib
- Dockerfile 없이 gradle에서 바로 이미지 만드는 구글 플러그인
- 베이스 이미지 받고 → 라이브러리 / 리소스 / 내 코드 레이어 나눠서 얹음 → 코드만 바뀌면 그 레이어만 다시 빌드
- `jibDockerBuild` = 내 도커에 넣음 / `jib` = 원격 저장소로 바로 푸시 (도커 꺼져있어도 됨)
- `from { image = "eclipse-temurin:25-jre" }` = Dockerfile의 FROM. jre라 jdk보다 가벼움
- buildscript에 `asm:9.8` 넣은 이유: Jib 3.4.4 기본 ASM이 Java 25 클래스파일 못 읽음 (main 클래스 찾을 때 씀)
- creationTime 안 정하면 이미지 생성시간이 1970년으로 찍힘
- `./gradlew --stop`은 컨테이너 아니고 Gradle 데몬 끄는 거
- `docker compose up -d`는 이미지 바뀐 서비스만 컨테이너 새로 만듦. `&&`는 앞에 성공해야 다음 실행

### 애로사항
- **compose up 했는데 coupon-service pull access denied**
  - coupon-service:latest는 내가 빌드해야 하는 이미지인데 안 만들어서 도커허브에서 찾다가 실패
- **3306 address already in use**
  - `sudo lsof -nP -iTCP:3306 -sTCP:LISTEN` → mysqld / USER `_mysql` → 옛날에 공식 dmg로 깔아둔 로컬 MySQL
  - brew로 깐 게 아니라 brew services stop 안 먹힘
  - `sudo launchctl unload -w /Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist` (-w: 부팅 때도 안 켜짐)
- **existByUserIdAndCouponId 오타** → existsBy 여야 함
  - 스택트레이스는 맨 아래 Caused by부터 보기
  - 키워드 인식 안 되니까 스프링이 `existByUserId`를 필드명으로 착각 → "No property found"
- **import SoldoutException 오타** (O 대문자여야 함). 에러 3개였는데 원인은 1개 → 맨 위 에러부터 보기
- **jibDockerBuild 401 Unauthorized (진짜 개삽질)**
  - 처음엔 config.json에 Docker Hub 항목이 빈 껍데기로 남아있었음
  - docker login 해도 계속 401 → 키체인에 Docker Hub 정보가 두 개 있었음
    - `https://index.docker.io/v1/` → 새 로그인 정보 (docker가 씀)
    - `index.docker.io` → 옛날 만료된 정보 (Jib이 씀)
  - Jib은 registry-1.docker.io → registry.hub.docker.com → index.docker.io 순서로 찾고 처음 찾은 거만 씀. 401 나도 다음 후보 안 봄
  - `echo "index.docker.io" | docker-credential-desktop erase` 로 옛날 거 지움
  - docker pull은 되는데 Jib만 401이면 키체인 옛날 정보부터 의심
- **tags = "lastest" 오타** → latest. compose는 coupon-service:latest 찾음
- **api.sh 돌렸는데 8080 connection refused**
  - 포트 사용 중이 아니라 아무것도 안 떠있던 거
  - compose에서 redis 서비스는 지웠는데 depends_on에 redis 남아있었음 → coupon-service가 created 상태로 시작 못함
- **내 발급목록 404 No static resource**
  - 스크립트는 /api/users/me/issuances, 컨트롤러는 /api/issuances → 주소 불일치
  - 매칭되는 컨트롤러 없으면 스프링이 정적 파일인지 찾다가 404
- **DB에서 한글 ?? 로 보임**
  - HEX 찍어보니 UTF-8로 제대로 저장돼 있었음. 컨테이너 mysql 클라이언트 문자셋이 latin1이라 표시만 깨진 거
  - `--default-character-set=utf8mb4` 또는 `SET NAMES utf8mb4;`
  - 앱(JDBC)은 알아서 utf8mb4로 통신해서 처음부터 정상
- Deprecated Gradle features 경고 → JDK 25 toolchain 자동 다운로드 위치 설정 없어서. 빌드는 됨. settings.gradle.kts에 foojay-resolver 플러그인 넣으면 없어짐

### 다음에 볼 것
- use()에서 userId 받아놓고 본인 쿠폰인지 검사 안 함
- 동시 요청 시 수량 초과 발급 / 쿠폰 중복 사용 → 동시성 (일정 프젝 Redisson 락이랑 같은 주제)


---
동시성 (비관적 락, Redis 락/원자연산 개념, 부하 테스트)
---

### 한 것
- 부하 테스트
  - 1. 기본 아무것도 안함 > 성능 테스트
  - 2. DB 비관락 (_WRITE) > 수정 & 성능 테스트 
  - 3. Redis 락
  - 4. Redis 원자연산 > 수정 & 성능 테스트

### 배운 것

#### 원자적 연산 / Redis
- 원자적 연산 = 중간에 아무도 못 끼어드는 한 덩어리 작업
- 재고 확인이랑 차감이 따로면 그 틈에 다른 요청이 끼어들어서 1장 남았는데 2명 발급됨
- Redis는 명령 하나씩 순서대로 처리(싱글스레드) → 명령 하나는 원자적. 여러 개면 Lua로 묶음
- 여기서 Redis는 캐시 아님. 재고 판정의 기준(원본)
- Redis 서버 여러 대여도 키 하나는 마스터 하나에만 있음 → 앱 서버 몇 대든 같은 값 봄
  - Cluster에서 Lua로 키 여러 개 쓰려면 `{coupon:1}` 해시태그로 같은 서버에 모아야 함
- 왜 빠름? 비관적 락은 락 잡은 채로 SELECT/UPDATE/INSERT/COMMIT 다 함(수~수십 ms), Redis는 메모리 명령 하나(0.1ms 이하)
  - 같은 쿠폰 요청은 한 줄로 서야 하니까 임계영역 시간 = 처리량 한계
  - 기다리는 요청들이 DB 커넥션 붙잡고 있음 → 풀 바닥 → 다른 API까지 멈춤
  - 경쟁은 같은 쿠폰 노리는 모든 유저끼리
- 질문...완벽한가? 아님. Redis랑 DB가 한 트랜잭션이 아님
  - Redis 차감 성공 → DB 저장 전에 뻑남 → 1장 묶임, B는 품절 = 덜 발급
  - Redis 재시작(AOF 1초 유실) / 마스터 장애 전환(복제 비동기) → 재고 되살아남 = 초과 발급
  - 대처: DB 에러면 catch에서 Redis +1 보상 / 앱 죽으면 배치로 Redis vs DB 비교 보정 / 더 확실하게는 큐
  - DB가 최종 방어선: UNIQUE(user_id, coupon_id), `UPDATE ... WHERE issued_quantity < total_quantity`
- 판단 기준: 초과 발급(돈 나감)은 절대 안 됨, 덜 발급은 드물고 보정 가능 → 감수. 정답이 아니라 트레이드오프

#### 기본 구현이 왜 초과 발급되나 (lost update)
- 다 같이 동시에 issued_quantity 읽음(0) → 다 같이 "남았네" 판단 → 각자 0+1=1로 덮어씀
- 발급 행(INSERT)은 추가라서 다 남고, 카운터(UPDATE)는 덮어써서 +1이 뭉개짐
  - 그래서 카운터는 실제보다 적고(강의 2,820) 발급 행은 한도 초과(강의 19,899)
- 틀린 카운터 믿고 isSoldOut() 판단하니까 계속 발급됨
- JPA 변경 감지는 `issued = issued + 1` 이 아니라 코틀린에서 계산한 값을 그대로 넣음 → 덮어쓰기
  - SQL로 `issued = issued + 1` 하면 카운터는 맞아지는데 판정은 옛날 값이라 초과 발급은 그대로

#### 트랜잭션 vs 락 (진짜 헷갈림)
- 트랜잭션 = "망가지지 않게". 중간에 실패하면 전부 롤백
- 락 = "동시에 못 건드리게". 한 명씩 줄 세움
- @Transactional 있어도 lost update 남. MySQL 기본(REPEATABLE READ)에서 일반 SELECT는 락 없이 스냅샷 읽음
- ATM 비유: 트랜잭션은 송금 끝까지 하거나 아예 안 하거나, 락은 ATM 앞에 한 명씩 줄 서기

#### @Query / @Lock (비관적 락)
- `@Query` = 쿼리 내가 직접 적을게. 메서드 이름 규칙으로 안 되거나 락 옵션 붙일 때
- JPQL: 테이블 아니라 엔티티 기준. `FROM Coupon c` 는 클래스 이름, `c.id` 는 필드
- `:id` = 파라미터 들어갈 빈칸. `@Param("id")` 랑 이름 맞춰야 연결됨
  - 따옴표 안의 `:` 는 JPQL 빈칸, 밖의 `:` 는 코틀린 타입
- `@Lock` 없으면 그냥 findById 랑 똑같음. 반환 타입 없으면 Unit이라 결과 못 받음 → `: Coupon?`
- PESSIMISTIC_READ = 공유 락 (`FOR SHARE`). 여럿이 같이 잡을 수 있음
  - 쿠폰에 쓰면 A, B 둘 다 공유 락 잡고 → 서로 UPDATE 하려고 기다림 → 데드락
- PESSIMISTIC_WRITE = 배타 락 (`FOR UPDATE`). 혼자 잡음, 다른 트랜잭션은 읽기부터 대기
  - 읽은 값으로 수정할 때 → 재고 확인 후 차감이 딱 이 경우
- 락은 커밋 때 풀림 → @Transactional 안에서 불러야 함

#### 부하 테스트 스크립트
- 흐름: reset(비우기) → create_coupon(쿠폰 만들기) → k6(때리기) → verify(채점)
- over_issuance.js: 2만 명 중 랜덤, 초당 5,000번 30초 → 초과 발급 보는 거
- duplicate_issuance.js: 1천 명이라 같은 사람이 여러 번 → 중복 발급 보는 거 (UNIQUE 때문에 항상 0)
- verify: over_issuance(실제 발급 > 재고면 FAIL), count_match(카운터 ≠ 실제 발급이면 FAIL)
- bash
  - `set -euo pipefail` 하나라도 실패하면 바로 멈춤 (k6 없어서 멈춘 것도 이거 덕분)
  - `cd "$(dirname "$0")/../.."` 스크립트 위치 기준으로 coupon/ 으로 이동 → 폴더 한 단계 깊어지면 `../../..` 로 고쳐야 함
  - `$(명령)` 결과를 값으로 씀
  - `k6 run -e KEY=값` → JS에서 `__ENV.KEY`
- 언어가 bash + SQL + JS 세 개 섞여 있어서 어려운 거 당연. 파일이 뭐 하는 건지만 알면 됨

#### k6 결과 (중점으로 봐야할거 3개)
1. 정확했나 → verify 결과 (FAIL이면 속도 볼 필요 없음)
2. 계획만큼 보냈나 → http_reqs, dropped_iterations
3. 빨랐나 → p(95), http_req_failed
- 깔때기: 계획 15만 = dropped(아예 못 보냄) + http_reqs(보냄) / http_reqs = failed(보냈는데 실패) + 성공
- VU = Virtual User...가상 사용자. 한 VU는 응답 올 때까지 요청 하나에 묶임
  - `Insufficient VUs` 경고 = VU 다 응답 기다리는 중이라 새 요청 못 보냄 → dropped
  - 필요한 VU 수 = 초당 요청 × 응답 시간 → 5,000 × 37초 = 18.5만 → 불가능
  - VU 늘려봤자 서버 대기줄만 길어짐. dropped 크면 "서버가 못 버팀"으로 읽기
- P95 = 95%의 요청이 이 시간 안에 응답받음

#### 성능 테스트 결과
| | 보낸 요청 | 발급 | 카운터 | p95 | 판정 |
|---|---|---|---|---|---|
| 기본 구현 | 6,066 (101/s) | 5,535 | 593 | 53.9s | 초과 + 불일치 |
| 비관적 락 | 3,181~3,952 (52~65/s) | 3,078~3,687 | 발급이랑 같음 | 54~56s | PASS |
- 비관적 락: 정확해졌는데 처리량 절반. 한 행 락을 한 줄로 기다리는 대가
- 근데 5,000장까지 못 가서 품절 경계는 검증 안 됨 (반쪽 PASS)
- 강의 결과 Redis가 비관락보다 24% 빠른 정도(3.67s → 2.8s)인 건 다른 병목(쿠폰 조회 DB, 앱 서버)이 남아있다는 신호일 수 있음

### 애로사항
- **k6: command not found** → `brew install k6`. 옆에 뜬 brew 경고들은 옛날 tap 얘기라 무시
- **부하가 계획의 4%만 들어감** (15만 중 6천)
  - 네트워크 아님 (localhost, 27 kB/s, min 24ms)
  - Hikari 기본 풀 10개 + Docker VM CPU 4 / 메모리 3.8GB를 앱이랑 MySQL이 나눠 씀 + k6도 같은 노트북
  - 계산대 10개인데 손님만 밀어넣은 상황
- **verify 표(5,535)랑 아래 줄(5,588)이 다름** → k6 끝나도 서버는 받은 요청 계속 처리 중. verify가 SQL 두 번 돌리는 사이에 늘어남 → k6 뒤에 sleep 필요
- **재고 500으로 바꿨는데 total_quantity 5000** → CreateCouponRequest에 `totalQuality` 오타
  - JSON의 totalQuantity를 못 찾음 → 스프링은 모르는 필드 조용히 무시 → 기본값 5000
  - 에러 안 나서 2강 때부터 계속 이랬음 (api.sh 100 보낸 것도 5000으로 들어가 있었음)
  - 자동완성이 Quality 보고 `javax.print...PrintQuality` import까지 해놨었음
  - 기본값 있는 필드는 오타 나도 조용히 동작해서 버그가 숨음
- 재고는 그냥 5000으로 통일하기로 함

### 다음에 볼 것
- part-2-3-redis-lua 브랜치
- README TODO: rate 낮춰서 세 브랜치 같은 조건 비교, verify 전 sleep, docker stats, 풀/스레드 튜닝, 재고 500으로 품절 경계 검증


---
Redis 원자연산 + Lua 
---

### 한 것
- spring-boot-starter-data-redis 추가, compose에 redis 다시 넣음
- CouponIssuer(@Component) + lua/issue.lua 로 재고 판정을 Redis로 옮김
- 쿠폰 생성할 때 initStock 으로 Redis에 재고 적기
- 발급 수량은 `UPDATE ... SET issued = issued + 1` (@Modifying) 로 DB가 직접 +1
- 부하 테스트 PASS

### 배운 것

#### CouponIssuer
- 한 줄 요약: "쿠폰 줄 수 있어?"를 DB 대신 Redis한테 물어보는 담당자
- Redis에 공책 두 권: `coupon:1:stock` (남은 개수), `coupon:1:users` (받아간 사람 명단, Set)
  - stockKey / usersKey 는 키 이름만 만드는 함수. 보내는 건 아님
  - `"coupon:$couponId:stock"` → `$변수` 는 문자열 안에 값 끼워넣는 코틀린 문법
- initStock = 쿠폰 **생성**할 때 한 번 재고 적기 (`SET coupon:1:stock 5000`)
  - 쿠폰 id는 키 이름 안에 들어가고 수량이 값
  - opsForValue() = 문자열 타입, opsForSet() = Set 타입
  - StringRedisTemplate 이라 값은 문자열로만 → toString(), Lua에서 tonumber()
- tryIssue = 쿠폰 **발급**할 때마다 Lua 실행 → 1 성공 / 0 품절 / -1 이미 받음 → when 으로 예외
  - 마이너스(DECR)는 Lua가 함
- `script` 필드 = lua 파일 미리 읽어둔 객체. 요청마다 다시 안 읽으려고
- @Component vs @Service: 기능 차이 없음 (@Service 안에 @Component 들어있음). 이름표 차이
  - @Service = 업무 흐름 전체 (CouponService), @Component = 그 안에서 쓰는 부품 (CouponIssuer는 Redis 판정 한 가지만)
  - @Repository 는 DB 예외 변환 기능 추가로 있음

#### Lua
- Lua 문법은 이 파일에서 5개뿐: `--` 주석, `local` 변수, `if then end`, `or`(= ?:), `redis.call()`
- SISMEMBER / GET / DECR / SADD 는 Lua가 아니라 Redis 명령. Lua는 포장지
  - 포장지로 감싸야 Redis가 스크립트 전체를 명령 하나처럼 한 번에 실행 (원자적)
  - 코틀린에서 명령 하나씩 보내면 그 사이에 끼어들 수 있음
- KEYS = Redis 키 이름들, ARGV = 그 밖의 값들 (ARGument Vector)
  - Cluster에서 어느 서버로 보낼지 알아야 해서 키는 반드시 KEYS로
  - Lua 배열은 1부터 시작
- 지금 버전 Lua는 재고만 봄. 중복 유저는 DB(existsBy + UNIQUE)가 막음

#### Coupon vs Issuance
- coupon 테이블 = 쿠폰 종류(행사). 쿠폰 하나당 한 줄. 5000장이어도 id는 1개
- issuance 테이블 = 유저한테 실제로 나간 한 장. 발급마다 한 줄 (coupon_id로 어떤 쿠폰인지)
- 영화 비유: coupon = 상영 하나(좌석 5000), issuance = 관객 티켓
- 부하 테스트는 쿠폰 하나만 만들어서 다 같이 그 한 줄 두고 경쟁 → 비관락이 느린 이유

#### @Modifying
- @Query 는 기본이 SELECT라고 가정 → UPDATE/DELETE 쓰려면 @Modifying 붙여야 executeUpdate()로 실행
- 반환 Int = 바뀐 행 수. @Transactional 안에서 불러야 함
- DB를 직접 바꿔서 이미 조회해둔 엔티티 객체엔 반영 안 됨 → 다시 읽을 거면 `clearAutomatically = true`
- `coupon.id!!` = null 아니라고 강제. null이면 그 자리에서 예외 (requireNotNull 이랑 같은 역할)

#### 결과
- PASS. 보낸 요청 4,245 (70/s), 발급 3,944, p95 55s
- 비관락(52~65/s, 발급 3,000~3,700)보다 조금 나음. 강의도 24% 차이라 방향은 맞음
- 차이가 작은 이유 (네트워크 아님)
  - 품절이 안 됨 → 다 성공 경로 → 성공 건은 결국 DB UPDATE + INSERT 해야 해서 DB 작업량 비슷. 줄어든 건 락 대기뿐
  - Redis 판정 전에 DB 조회가 2번 (findById, existsBy) → 거절될 요청도 DB 커넥션 다 씀
  - 요청 하나가 DB 4번 쓰는데 Hikari 10개 + Docker CPU 4개는 그대로
- Redis 진짜 장점 = 실패할 요청을 DB까지 안 보내는 것 → 품절 이후에 드러남

### 애로사항
- **StringRedisTemplate, RedisScript import 안 됨** → build.gradle.kts에 redis starter 없었음. 추가하고 Gradle 다시 불러와야 IntelliJ가 인식
- **issuedQuantitiy 오타** (i 하나 더) → JPQL은 필드 이름 써야 해서 앱 켜질 때 터짐
- **run.sh 돌렸는데 curl: (52) Empty reply from server**
  - 앱 켜지는 데 14초 걸리는데 그 전에 요청 보냄. 포트는 열려있는데 받아줄 앱이 아직 없음
  - `docker compose up -d` 는 컨테이너만 켜지면 끝남. 로그에서 `Started CouponApplicationKt` 확인하고 돌리기
- 커밋 메시지 고치다가 IntelliJ reword랑 동시에 해서 꼬일 뻔함. 기록 바꿀 땐 한 군데서만

---
2단원 정확성 정리
---

| | 방식 | 보낸 요청 | 발급 | 카운터 | p95 | 판정 |
|---|---|---|---|---|---|---|
| part-2-1 | 기본 구현 | 6,066 (101/s) | 5,535 | 593 | 53.9s | 초과 발급 + 카운터 불일치 |
| part-2-2 | 비관적 락 | 3,181~3,952 (52~65/s) | 3,078~3,687 | 발급이랑 같음 | 54~56s | PASS |
| part-2-3 | Redis + Lua | 4,245 (70/s) | 3,944 | 발급이랑 같음 | 55s | PASS |

- 기본 구현: 다 같이 동시에 읽고 덮어씀 → lost update → 카운터 적게 셈 → 틀린 카운터 믿고 초과 발급
- 비관적 락: `FOR UPDATE` 로 한 줄 세움 → 정확. 대신 한 행 락을 다 기다려서 처리량 떨어짐
- Redis + Lua: 판정만 Redis 한 방에 원자적으로 → 정확. 임계영역이 짧아서 조금 빠름
  - 근데 Redis-DB가 한 트랜잭션 아님 → 장애 땐 덜 발급 가능 (초과 발급은 DB 제약으로 막기)
- 한 줄: 트랜잭션은 "망가지지 않게", 락은 "동시에 못 건드리게". 정확성은 둘 다 필요


---
## TODO (한 번에 할 것)
---

### 코드
- [ ] `use()` 에서 userId 받아놓고 본인 쿠폰인지 검사 안 함 → NotOwnerException 정의만 돼있고 안 씀
- [ ] UserIssuanceController 주소 확인 (`/api/issuances` vs api.sh 의 `/api/users/me/issuances`)
- [ ] Redis 판정을 issue() 맨 앞으로 옮겨보기 (거절될 요청이 DB 안 거치게) → 차이 측정
- [ ] **(먼저) Lua에서 유저 중복 체크** → `SISMEMBER` 로 명단 확인해서 있으면 -1, 발급 성공 시 `SADD` 로 명단 추가
  - tryIssue 에 userId 다시 넘기고 KEYS 2개(재고, 명단) + ARGV(userId) 맞추기
  - part-3-1 에서 실제로 터짐: 같은 유저 동시 2번 → existsBy 둘 다 통과 → Lua가 재고만 봐서 둘 다 DECR → 두 번째 INSERT UNIQUE 위반 → DB는 롤백, Redis 재고는 안 돌아옴 → 4,999장
- [ ] Redis 차감 성공 후 DB 저장 실패 시 보상 처리 (catch 에서 Redis 재고 +1, 명단에서 제거)
  - @Transactional 은 메서드 끝날 때 커밋이라 커밋 실패는 catch 에 안 걸릴 수 있음 → 트랜잭션 밖에서 감싸거나 TransactionSynchronization
  - 3단원 큐 + 워커로 가면 DB 저장이 워커로 옮겨가서 모양 달라짐 → 강의 보고 맞추기
- [ ] UNIQUE 위반(`DataIntegrityViolationException`) 잡아서 500 대신 409 (AlreadyIssuedException) 로
  - 지금은 "중복 차단"만 하고 "멱등 처리(중복은 성공으로 흡수)"는 안 하는 상태
- [ ] DB 최종 방어선: `UPDATE ... WHERE issued_quantity < total_quantity` 로 초과 발급 차단
- [ ] 쿠폰 사용(use) 동시 요청 시 중복 사용 → 동시성 처리
- [ ] Issuance status `@Column(length = 16)` 은 MySQL ENUM이라 의미 없음 → VARCHAR로 할지 결정
- [x] Lua 유저 중복 체크 → part-3-2a 에서 추가, 3-2b 부하 테스트에서 정확히 5,000 확인
- [ ] 요청 경로의 `findById` DB 조회 없애기 (쿠폰 정보 캐시 or Redis 판정 맨 앞으로)
  - 3-2b: 품절 뒤 1만 건도 전부 DB 커넥션 쓰고 나서 409, 워커 쓰기랑 커넥션 10개 나눠 씀 → p99 21.75s (목표 0.5s)
- [ ] @Async 스레드 풀 큐(10,000) 가득 차면 TaskRejectedException → 지금 500으로 나감. QueueFullException(503) 지우면서 처리 빠짐
- [ ] 워커(@Async) DB 저장 실패 시 재시도 없음, 로그만 남고 이벤트 버려짐 → Redis 재고만 빠진 채로 남음

### 부하 테스트
- [ ] 재고 500으로 줄여서 품절 경계 검증 (issuance_rows 가 정확히 500에서 멈추는지) → 세 브랜치 다
- [ ] rate 를 로컬에서 버티는 수준(300~500)으로 낮춰서 세 브랜치 같은 조건 비교
- [x] run.sh 에서 k6 끝나고 sleep 넣고 verify (서버가 남은 요청 처리 중이라 숫자 바뀜) → part-3 run.sh 에 Worker 드레인 대기로 들어감
- [ ] run.sh 에 앱 준비될 때까지 기다리는 로직 (Empty reply 방지)
- [x] Redis 명단(users)까지 Lua에서 쓰게 되면 reset.sh 에 `redis-cli FLUSHDB` 추가 → part-3-1 에서 추가됨
- [ ] 테스트 중 `docker stats` 로 앱/MySQL CPU 확인
- [ ] Hikari 풀 크기, 톰캣 스레드 수 튜닝 (무작정 키우면 MySQL이 못 버팀)
- [ ] 품절 이후 요청 응답시간, P50/P99, Hikari pending 커넥션 수 같이 보기
- [ ] **part-3-2a 다시 측정**: 10/12 이후 jibDockerBuild 가 실패해서 옛날 이미지로 돌았음 (2a 결과는 무효)
  - 테스트 전에 `BUILD SUCCESSFUL` + `docker images coupon-service` 생성 시간 + Redis `coupon:1:users` 키 생기는지 확인

### 환경 / 설정
- [x] show-sql: true 지우기 (SQL 로그 두 번 찍힘) → part-3-1 에서 show-sql false + SQL/bind 로그 WARN 으로 줄임 (부하 테스트 latency 에 영향 컸음)
- [ ] settings.gradle.kts 에 foojay-resolver 플러그인 (Gradle 10 경고)
- [ ] 안 쓰는 brew tap 정리 (`brew untap adoptopenjdk/openjdk homebrew/cask-versions`)

### 공부
- [ ] 코틀린 문법 몰아서 정리 (지금은 개념 위주로 넘어감)
- [ ] Redis 장애 시나리오 (AOF 유실, 레플리카 전환) 면접 대비 정리
