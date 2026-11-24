# hr-reward

소규모 매장·회사용 HR + 직원 보상(쿠폰) 서비스. 포트폴리오 겸 실제 서비스 목표.

## 사용자와 역할
- 플랫폼 관리자: 서비스 운영자. 별도 관리자 웹, platform_admins 테이블로 분리
- 매장 관리자(OWNER, MANAGER) / 직원(EMPLOYEE)
- 계정(users)은 1인 1개, 역할은 store_members에서 매장별로 부여
- 직원은 사장님이 발급한 초대 코드로 가입 → 승인 후 ACTIVE

## 기술 스택
- Java 21, Spring Boot, Gradle(Groovy)
- Spring Web, Spring Data JPA, Spring Security(JWT), Validation
- MySQL 8 (로컬은 docker-compose), Flyway로 스키마 관리
- 추후: Redis, QueryDSL, springdoc-openapi

## 패키지 구조
- com.seungwon.hrreward.global: config, auth, exception, common
- 도메인별 패키지: user, store, schedule, attendance, reward, coupon, payment
- 각 도메인: controller / service / repository / domain / dto

## 계층 구조 (반드시 지킬 것)
모든 도메인은 아래처럼 계층을 나눠서 작성한다.

예시 (store 도메인):
```
store/
├── controller/   StoreController.java
├── service/      StoreService.java
├── repository/   StoreRepository.java, StoreMemberRepository.java
├── domain/       Store.java, StoreMember.java, StoreRole.java (enum)
└── dto/
    ├── request/  StoreCreateRequest.java
    └── response/ StoreResponse.java
```

### Controller
- 요청을 받고 응답을 반환하는 역할만 한다
- 요청 DTO 검증(@Valid), 로그인 사용자 정보 전달, Service 호출만 한다
- 비즈니스 로직, Repository 직접 호출 금지
- 엔티티를 직접 반환하지 않고 응답 DTO로 변환된 값을 반환한다

### Service
- 비즈니스 로직과 트랜잭션을 담당한다
- 클래스에 @Transactional(readOnly = true), 쓰기 메서드에만 @Transactional
- 엔티티 ↔ DTO 변환은 Service 또는 DTO의 정적 팩토리 메서드(from, of)에서 한다
- 다른 도메인의 데이터가 필요하면 그 도메인의 Repository가 아닌 Service를 통해 접근한다

### Repository
- Spring Data JPA 인터페이스로 데이터 접근만 담당한다
- 비즈니스 로직 금지
- 복잡한 조회는 나중에 QueryDSL용 Custom Repository로 분리한다

### Domain (Entity)
- 상태 변경 규칙은 엔티티의 도메인 메서드에 둔다 (예: storeMember.approve(), storeMember.resign())
- @Setter 금지, 기본 생성자는 protected

### DTO
- record로 작성하고 request / response 패키지로 나눈다

## 코딩 규칙
- 엔티티에 @Setter 금지. 상태 변경은 의미 있는 도메인 메서드로
- 엔티티 기본 생성자는 protected
- DTO는 record 사용, 요청 DTO에 Bean Validation 적용
- 컨트롤러는 엔티티를 직접 반환하지 않음
- 예외는 ErrorCode enum + BusinessException, GlobalExceptionHandler에서 처리
- 금액 계산은 BigDecimal 사용
- JPA ddl-auto는 validate. 테이블 변경은 반드시 Flyway 마이그레이션 파일 추가
- 권한 체크는 역할 + 매장 소속을 함께 확인 (다른 매장 데이터 접근 금지)

## 작업 방식
- 큰 작업은 먼저 계획을 제시하고 확인받은 뒤 구현
- 기능을 구현할 때 테스트 코드를 함께 작성
- 설계 선택에 이유가 있으면 짧게 설명
- 기능 단위로 feat/xxx 브랜치를 만들어 작업 (main에 직접 커밋하지 않음)
- 작업이 끝나면 테스트 통과 확인 → 커밋 → 푸시 → PR 제목과 본문 작성
