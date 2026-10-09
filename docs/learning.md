---
2026-10-06 ~ 10-08 — 1~2강 (프젝 세팅, 쿠폰 발급/사용 API, Jib 이미지 빌드)
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
