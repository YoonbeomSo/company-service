# 회식투표 서비스 - 수용 테스트 체크리스트

> 작성일: 2026-03-23
> 목적: 요구사항 대비 구현 상태 검증 및 QA/Playwright 테스트 시나리오 제공
> Base URL: `http://localhost:8080` (profile: local, port 미지정 시 기본 8080)

---

## 1. 인증 (Auth)

### 1-1. 로그인 페이지 진입

| 항목 | 내용 |
|------|------|
| **요구사항** | 첫 로딩페이지에서 이름 + 비밀번호(숫자 4자리) 입력 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `AuthController.loginPage()` -> `GET /login` -> `auth/login.html` |
| **비고** | `MainController`에서 `GET /` -> `redirect:/restaurants`. `AuthInterceptor`가 세션 없으면 `/login`으로 리다이렉트 |

#### Playwright 시나리오

```
TC-AUTH-01: 비로그인 상태에서 루트 접근 시 로그인 페이지로 리다이렉트
1. GET / 접근
2. /login 으로 리다이렉트 확인
3. input[name="name"], input[name="password"] 존재 확인
4. password 입력 필드: pattern="[0-9]{4}", maxlength="4", inputmode="numeric" 속성 확인
5. "시작하기" 버튼 존재 확인
6. "처음이시면 자동으로 계정이 생성됩니다" 안내 문구 확인
```

---

### 1-2. 신규 사용자 자동 계정 생성

| 항목 | 내용 |
|------|------|
| **요구사항** | 없는 이름이면 계정 생성 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `AuthService.loginOrRegister()` - `memberRepository.findByName(name)` 결과가 null이면 `Member.create()` 후 `save()` |
| **비고** | 비밀번호는 `BCryptPasswordEncoder`로 해싱 저장 |

#### Playwright 시나리오

```
TC-AUTH-02: 신규 사용자 - 이름/비밀번호 입력 시 계정 생성 후 메인으로 이동
1. GET /login 접속
2. input[name="name"]에 "테스트유저A" 입력
3. input[name="password"]에 "1234" 입력
4. "시작하기" 버튼 클릭
5. /restaurants 페이지로 리다이렉트 확인 (로그인 성공)
6. 헤더 영역에 "테스트유저A" 이름 표시 확인 (session.memberName)
```

---

### 1-3. 기존 사용자 비밀번호 확인

| 항목 | 내용 |
|------|------|
| **요구사항** | 있는 이름이면 비밀번호 확인 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `AuthService.loginOrRegister()` - `passwordEncoder.matches(rawPassword, existingMember.password)` |

#### Playwright 시나리오

```
TC-AUTH-03: 기존 사용자 - 올바른 비밀번호로 로그인 성공
사전조건: TC-AUTH-02로 "테스트유저A" / "1234" 계정 생성 완료
1. GET /login 접속
2. input[name="name"]에 "테스트유저A" 입력
3. input[name="password"]에 "1234" 입력
4. "시작하기" 버튼 클릭
5. /restaurants 페이지로 리다이렉트 확인
6. 헤더에 "테스트유저A" 표시 확인
```

---

### 1-4. 비밀번호 불일치 시 에러 메시지

| 항목 | 내용 |
|------|------|
| **요구사항** | 비밀번호 불일치 시 에러 메시지 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `ErrorType.PASSWORD_MISMATCH` -> "비밀번호가 일치하지 않습니다". `AuthController.login()`에서 `CoreException` catch 후 `model.addAttribute("errorMessage", ...)` |
| **비고** | 에러 시 login 페이지 재렌더링, 이름 값 유지 (`model.addAttribute("name", request.name)`) |

#### Playwright 시나리오

```
TC-AUTH-04: 기존 사용자 - 잘못된 비밀번호 입력 시 에러 메시지 표시
사전조건: "테스트유저A" / "1234" 계정 존재
1. GET /login 접속
2. input[name="name"]에 "테스트유저A" 입력
3. input[name="password"]에 "5678" 입력
4. "시작하기" 버튼 클릭
5. 여전히 /login 페이지에 머무름 확인 (리다이렉트 없음)
6. "비밀번호가 일치하지 않습니다" 에러 메시지 표시 확인
7. input[name="name"]에 "테스트유저A" 값이 유지되는지 확인
```

---

### 1-5. 유효성 검증 - 빈 이름

| 항목 | 내용 |
|------|------|
| **요구사항** | 이름 + 비밀번호 입력 필수 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `LoginRequest.name` - `@NotBlank(message = "이름을 입력해주세요")` |

#### Playwright 시나리오

```
TC-AUTH-05: 이름 미입력 시 유효성 검증 에러
1. GET /login 접속
2. input[name="name"]을 비워둠
3. input[name="password"]에 "1234" 입력
4. "시작하기" 버튼 클릭
5. HTML5 required 속성에 의해 브라우저 validation 발생 또는 서버 측 "이름을 입력해주세요" 에러 메시지 표시 확인
```

---

### 1-6. 유효성 검증 - 비밀번호 형식

| 항목 | 내용 |
|------|------|
| **요구사항** | 비밀번호는 숫자 4자리 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `LoginRequest.password` - `@Pattern(regexp = "^[0-9]{4}$", message = "비밀번호는 숫자 4자리여야 합니다")`. HTML input에 `pattern="[0-9]{4}" maxlength="4"` |

#### Playwright 시나리오

```
TC-AUTH-06: 비밀번호 형식 오류 - 문자 포함
1. GET /login 접속
2. input[name="name"]에 "테스트유저B" 입력
3. input[name="password"]에 "ab12" 입력 (JS로 pattern bypass 후 제출)
4. "비밀번호는 숫자 4자리여야 합니다" 에러 메시지 확인

TC-AUTH-07: 비밀번호 형식 오류 - 3자리
1. GET /login 접속
2. input[name="name"]에 "테스트유저B" 입력
3. input[name="password"]에 "123" 입력 (JS로 pattern bypass 후 제출)
4. "비밀번호는 숫자 4자리여야 합니다" 에러 메시지 확인
```

---

### 1-7. 로그아웃

| 항목 | 내용 |
|------|------|
| **요구사항** | (암묵적 요구) 로그아웃 기능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `AuthController.logout()` - `POST /logout` -> `session.invalidate()` -> `redirect:/login` |

#### Playwright 시나리오

```
TC-AUTH-08: 로그아웃 후 로그인 페이지로 이동, 이후 인증 필요 페이지 접근 불가
사전조건: 로그인 상태
1. 헤더 영역의 "로그아웃" 버튼 클릭
2. /login 페이지로 리다이렉트 확인
3. GET /restaurants 접속 시도 -> /login 리다이렉트 확인
```

---

### 1-8. 인증 인터셉터

| 항목 | 내용 |
|------|------|
| **요구사항** | 비인증 사용자는 기능 사용 불가 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `AuthInterceptor` - `/**` 패턴에 적용, `/login`, `/css/**`, `/js/**`, `/error` 제외 |

#### Playwright 시나리오

```
TC-AUTH-09: 비로그인 상태에서 보호된 경로 접근 시 리다이렉트
1. 세션 없이 GET /restaurants 접속 -> /login 리다이렉트 확인
2. 세션 없이 GET /votes 접속 -> /login 리다이렉트 확인
3. 세션 없이 GET /votes/1 접속 -> /login 리다이렉트 확인
```

---

## 2. 탭 네비게이션

### 2-1. 기본 탭이 "후보지"

| 항목 | 내용 |
|------|------|
| **요구사항** | 회식투표 탭이 default (현재는 후보지, 투표 2개 탭) |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `MainController.index()` - `GET /` -> `redirect:/restaurants`. 레이아웃의 `activeTab` 변수로 현재 탭 하이라이트 |
| **비고** | 요구사항은 "회식투표 탭이 default"인데, 구현은 `/` 접근 시 `/restaurants`(후보지)로 리다이렉트. 후보지 탭이 사실상 default 랜딩 |

#### Playwright 시나리오

```
TC-NAV-01: 로그인 후 후보지 탭이 기본 활성화
1. 로그인 수행
2. / -> /restaurants 리다이렉트 확인
3. "후보지" 탭에 활성 스타일 확인 (text-primary, border-primary 클래스)
4. "투표" 탭에 비활성 스타일 확인 (text-gray-500 클래스)
```

---

### 2-2. 탭 전환

| 항목 | 내용 |
|------|------|
| **요구사항** | 후보지, 투표 2개 탭 전환 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `layout/default.html` - `<a href="/restaurants">후보지</a>`, `<a href="/votes">투표</a>` |

#### Playwright 시나리오

```
TC-NAV-02: 탭 전환 동작 확인
사전조건: 로그인 상태
1. /restaurants 접속 -> "후보지" 탭 활성 확인
2. "투표" 탭 클릭 -> /votes 이동 확인
3. "투표" 탭 활성, "후보지" 탭 비활성 확인
4. "후보지" 탭 클릭 -> /restaurants 이동 확인
5. "후보지" 탭 활성 확인
```

---

### 2-3. 헤더 사용자 정보 표시

| 항목 | 내용 |
|------|------|
| **요구사항** | (암묵적) 현재 로그인 사용자 표시 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `layout/default.html` - `session.memberName` 표시 + 로그아웃 버튼 |

#### Playwright 시나리오

```
TC-NAV-03: 헤더에 사용자 이름 및 로그아웃 버튼 표시
사전조건: "테스트유저A"로 로그인
1. 헤더 영역에 "테스트유저A" 텍스트 존재 확인
2. "로그아웃" 버튼 존재 확인
```

---

## 3. 월별 후보지 등록

### 3-1. 월별 후보지 목록 조회

| 항목 | 내용 |
|------|------|
| **요구사항** | 월별 여러 개의 후보지 등록/조회 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `RestaurantController.list()` - `GET /restaurants?yearMonth=YYYY-MM`. `RestaurantService.findByYearMonth()` |
| **비고** | yearMonth 미지정 시 현재 월 기본값 (`YearMonthUtils.currentOrDefault`) |

#### Playwright 시나리오

```
TC-REST-01: 후보지 목록 조회 - 현재 월 기본 표시
사전조건: 로그인 상태
1. GET /restaurants 접속
2. 현재 월 표시 확인 (예: "2026년 3월")
3. 후보지가 없으면 "등록된 후보지가 없습니다" 메시지 확인
```

---

### 3-2. 월 네비게이션 (이전/다음 월)

| 항목 | 내용 |
|------|------|
| **요구사항** | 월별 관리 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `YearMonthUtils.prev()`, `next()`. 템플릿에서 `<` `>` 버튼으로 월 이동 |

#### Playwright 시나리오

```
TC-REST-02: 월 네비게이션 동작
사전조건: 로그인 상태, /restaurants 접속 (2026년 3월 기준)
1. "2026년 3월" 표시 확인
2. "<" 버튼 클릭 -> "2026년 2월" 표시, URL에 yearMonth=2026-02 확인
3. ">" 버튼 2회 클릭 -> "2026년 4월" 표시, URL에 yearMonth=2026-04 확인
```

---

### 3-3. 후보지 등록

| 항목 | 내용 |
|------|------|
| **요구사항** | 필드: 가게이름, 음식종류, Link, 등록자명 / 누구나 등록 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `RestaurantController.register()` - `POST /restaurants`. 등록자는 세션의 `memberName` 자동 사용. HTMX로 목록 부분 교체 (`hx-post`, `hx-target="#restaurant-list"`) |
| **비고** | 등록자명은 입력 필드가 아닌 세션에서 자동 추출. 링크는 선택 입력 |

#### Playwright 시나리오

```
TC-REST-03: 후보지 등록 성공 (링크 포함)
사전조건: "테스트유저A"로 로그인, /restaurants 접속
1. input[name="name"]에 "맛있는 삼겹살집" 입력
2. input[name="foodType"]에 "한식" 입력
3. input[name="link"]에 "https://example.com" 입력
4. "등록" 버튼 클릭
5. HTMX 응답으로 목록 갱신 확인 (페이지 새로고침 없이)
6. 목록에 "맛있는 삼겹살집" 항목 표시 확인
7. 음식종류 "한식" 배지 표시 확인
8. 등록자 "테스트유저A" 표시 확인
9. "링크" 텍스트 클릭 가능 확인 (target="_blank")
10. 등록 폼이 초기화되었는지 확인 (hx-on::after-request 리셋)

TC-REST-04: 후보지 등록 성공 (링크 미입력)
사전조건: 로그인 상태
1. input[name="name"]에 "초밥 오마카세" 입력
2. input[name="foodType"]에 "일식" 입력
3. input[name="link"]는 비워둠
4. "등록" 버튼 클릭
5. 목록에 "초밥 오마카세" 항목 표시 확인
6. "링크" 텍스트가 표시되지 않음 확인 (th:if="${restaurant.link != null}")
```

---

### 3-4. 후보지 등록 유효성 검증

| 항목 | 내용 |
|------|------|
| **요구사항** | 가게이름, 음식종류 필수 |
| **구현 상태** | 구현 완료 (부분적) |
| **관련 코드** | `RegisterRestaurantRequest` - `@NotBlank` 어노테이션. HTML `required` 속성 |
| **비고** | `RestaurantController.register()`에서 `@Valid`를 사용하지만 `BindingResult`를 받지 않으므로, 유효성 검증 실패 시 기본 Spring 에러 처리로 이동할 수 있음. HTMX 요청에서의 에러 핸들링이 명시적이지 않음 |

#### Playwright 시나리오

```
TC-REST-05: 가게이름 미입력 시 등록 불가
1. input[name="name"]을 비워둠
2. input[name="foodType"]에 "한식" 입력
3. "등록" 클릭
4. HTML5 required validation으로 브라우저 에러 또는 서버 에러 발생 확인

TC-REST-06: 음식종류 미입력 시 등록 불가
1. input[name="name"]에 "테스트 식당" 입력
2. input[name="foodType"]을 비워둠
3. "등록" 클릭
4. HTML5 required validation으로 브라우저 에러 또는 서버 에러 발생 확인
```

---

### 3-5. 여러 후보지 등록 확인

| 항목 | 내용 |
|------|------|
| **요구사항** | 월별 여러 개의 후보지 등록 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `RestaurantRepository.findAllByYearMonth()` 목록 반환 |

#### Playwright 시나리오

```
TC-REST-07: 동일 월에 여러 후보지 등록 후 목록 확인
사전조건: 로그인 상태, 현재 월 기준
1. "삼겹살집" / "한식" 등록
2. "초밥집" / "일식" 등록
3. "파스타집" / "양식" 등록
4. 목록에 3개 항목 모두 표시 확인
5. 각 항목에 가게이름, 음식종류, 등록자, 등록시간 표시 확인
```

---

### 3-6. 다른 사용자가 등록한 후보지 조회

| 항목 | 내용 |
|------|------|
| **요구사항** | 누구나 등록, 누구나 투표 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | 월별 조회 시 등록자 구분 없이 전체 반환 |

#### Playwright 시나리오

```
TC-REST-08: 다른 사용자가 등록한 후보지 확인
1. "유저A"로 로그인 -> "삼겹살집" 등록
2. 로그아웃
3. "유저B"로 로그인 -> /restaurants 접속
4. "유저A"가 등록한 "삼겹살집" 항목 표시 확인 (등록자: "유저A")
5. "유저B"도 추가 후보지 등록 가능 확인
```

---

### 3-7. 월별 후보지 분리 확인

| 항목 | 내용 |
|------|------|
| **요구사항** | 월별 관리 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `Restaurant.yearMonth` 필드 (DB 컬럼: `target_month`). 조회 시 yearMonth 파라미터로 필터링 |

#### Playwright 시나리오

```
TC-REST-09: 서로 다른 월의 후보지가 분리되어 표시됨
1. 2026-03 월에서 "3월 식당" 등록
2. ">" 버튼으로 2026-04 이동
3. "등록된 후보지가 없습니다" 확인
4. 2026-04에서 "4월 식당" 등록
5. "<" 버튼으로 2026-03 이동
6. "3월 식당"만 표시되고 "4월 식당"은 표시되지 않음 확인
```

---

## 4. 월별 투표

### 4-1. 투표 목록 조회

| 항목 | 내용 |
|------|------|
| **요구사항** | 한 달에 여러 투표 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteController.list()` - `GET /votes?yearMonth=YYYY-MM`. `VoteService.findByYearMonth()` |

#### Playwright 시나리오

```
TC-VOTE-01: 투표 목록 조회
사전조건: 로그인 상태
1. "투표" 탭 클릭 -> /votes 이동
2. 현재 월 표시 확인
3. 투표가 없으면 "등록된 투표가 없습니다" 메시지 확인
```

---

### 4-2. 투표 생성

| 항목 | 내용 |
|------|------|
| **요구사항** | 그달의 후보지를 가져와서 투표 생성 / 최대 선택 가능 수 설정 / 마감시간 설정 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteController.create()` - `POST /votes`. `VoteService.createVote()` - 해당 월 후보지를 `restaurantRepository.findAllByYearMonth()`로 가져와 `VoteItem`으로 변환 |
| **비고** | 투표 제목, 마감시간(`datetime-local`), 최대 선택 수(기본값 1) 입력 필수 |

#### Playwright 시나리오

```
TC-VOTE-02: 투표 생성 성공
사전조건: 현재 월에 후보지 2개 이상 등록됨
1. "투표" 탭 클릭
2. input[name="title"]에 "3월 1차 회식 투표" 입력
3. input[name="deadline"]에 미래 시간 입력 (예: 2026-03-25T18:00)
4. input[name="maxSelections"]에 "2" 입력
5. "투표 만들기" 버튼 클릭
6. /votes/{id} 상세 페이지로 리다이렉트 확인
7. 투표 제목 "3월 1차 회식 투표" 표시 확인
8. "진행중" 배지 표시 확인
9. 마감시간 표시 확인 (2026-03-25 18:00)
10. "최대 2개 선택" 표시 확인
11. 등록된 후보지들이 체크박스 목록으로 표시 확인
```

---

### 4-3. 투표 생성 시 해당 월 후보지만 포함

| 항목 | 내용 |
|------|------|
| **요구사항** | 그달의 후보지를 가져와서 투표 생성 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteService.createVote()` 내부: `restaurantRepository.findAllByYearMonth(yearMonth)` 호출 후 `VoteItem` 생성 |

#### Playwright 시나리오

```
TC-VOTE-03: 투표 생성 시 해당 월 후보지만 포함됨
사전조건: 2026-03에 "A식당", 2026-04에 "B식당" 등록
1. /votes?yearMonth=2026-03 에서 투표 생성
2. 투표 상세에서 "A식당"만 목록에 표시, "B식당"은 없음 확인
```

---

### 4-4. 투표 생성 이후 추가된 후보지 미포함

| 항목 | 내용 |
|------|------|
| **요구사항** | 투표 생성 이후 추가된 후보지는 해당 투표에 포함 안됨 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteService.createVote()`에서 투표 생성 시점의 후보지 스냅샷을 `VoteItem`으로 저장. 이후 추가된 `Restaurant`는 기존 `Vote`의 `voteItems`에 포함되지 않음 |

#### Playwright 시나리오

```
TC-VOTE-04: 투표 생성 후 추가된 후보지가 해당 투표에 반영되지 않음
사전조건: 2026-03에 "A식당" 등록 상태
1. 2026-03 투표 생성 (투표 ID: X)
2. 투표 상세(GET /votes/X)에서 "A식당"만 표시 확인
3. 후보지 탭으로 이동 -> "B식당" 신규 등록
4. 다시 GET /votes/X 접속
5. 여전히 "A식당"만 표시되고 "B식당"은 없음 확인
```

---

### 4-5. 한 달에 여러 투표

| 항목 | 내용 |
|------|------|
| **요구사항** | 한 달에 여러 투표 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `Vote` 엔티티에 월별 유니크 제약 없음. `findAllByYearMonth` 목록 반환 |

#### Playwright 시나리오

```
TC-VOTE-05: 동일 월에 여러 투표 생성
사전조건: 2026-03에 후보지 등록됨
1. "3월 1차 회식" 투표 생성
2. /votes 목록으로 돌아가기
3. "3월 2차 회식" 투표 생성
4. /votes 목록에 2개 투표 표시 확인
5. 각각 클릭하면 별도 상세 페이지로 이동 확인
```

---

### 4-6. 투표 목록에서 상태 배지 표시

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감 전/후 구분 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `Vote.isExpired()` -> `vote/list.html`에서 "마감" / "진행중" 배지 |

#### Playwright 시나리오

```
TC-VOTE-06: 투표 목록에서 진행중/마감 상태 표시
사전조건: 미래 마감 투표 1개, 과거 마감 투표 1개 존재
1. /votes 접속
2. 미래 마감 투표에 "진행중" 배지 (bg-primary/10, text-primary) 확인
3. 과거 마감 투표에 "마감" 배지 (bg-gray-100, text-gray-500) 확인
4. 각 투표에 마감시간, 생성자 이름 표시 확인
```

---

### 4-7. 투표하기 (장소 선택)

| 항목 | 내용 |
|------|------|
| **요구사항** | 누구나 투표 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteController.castBallot()` - `POST /votes/{id}/ballot`. `VoteService.castBallot()` - 기존 투표 삭제 후 재투표 (delete + save) |

#### Playwright 시나리오

```
TC-VOTE-07: 장소 투표 수행
사전조건: 진행중인 투표 존재, 후보지 A/B/C, maxSelections=2
1. GET /votes/{id} 접속
2. "A식당" 체크박스 선택
3. "B식당" 체크박스 선택
4. "투표하기" 버튼 클릭
5. 페이지 새로고침 후 "A식당", "B식당" 체크박스 체크 상태 확인
6. "A식당" 옆 "1표", "B식당" 옆 "1표" 표시 확인
```

---

### 4-8. 최대 선택 수 초과 방지

| 항목 | 내용 |
|------|------|
| **요구사항** | 투표 생성 시 최대 선택 가능 수 설정 |
| **구현 상태** | 구현 완료 (서버 측 검증) |
| **관련 코드** | `Vote.validateCanVote(selectedCount)` - `selectedCount > maxSelections` 시 `CoreException(ErrorType.VOTE_LIMIT_EXCEEDED)` |
| **비고** | 클라이언트 측(JS) 제한은 없음. 서버에서만 검증. 초과 시 에러 페이지로 이동 |

#### Playwright 시나리오

```
TC-VOTE-08: 최대 선택 수 초과 시 에러
사전조건: maxSelections=1인 투표, 후보지 A/B 존재
1. GET /votes/{id} 접속
2. "A식당", "B식당" 모두 체크 (maxSelections=1이지만 클라이언트 제한 없음)
3. "투표하기" 클릭
4. 에러 페이지에 "최대 선택 수를 초과했습니다" 메시지 확인
```

> **QA 참고 - 개선 포인트**: 클라이언트 측에서 최대 선택 수를 넘지 못하도록 JavaScript 제한이 없음. 사용자 경험 개선 필요 가능성.

---

### 4-9. 마감 전 투표 변경

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감 전 투표 변경 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteService.castBallot()` - 기존 투표를 `deleteAll` 후 새로 `saveAll`. 장소/날짜 모두 동일 방식 |

#### Playwright 시나리오

```
TC-VOTE-09: 투표 변경 (재투표)
사전조건: 진행중인 투표, A/B/C 후보지, maxSelections=2
1. A, B에 투표
2. 투표 상세 페이지에서 A, B 체크 상태 확인
3. A 체크 해제, C 체크 선택
4. "투표하기" 클릭
5. B, C만 체크 상태, A는 미체크 확인
6. 득표 수: A=0, B=1, C=1 확인
```

---

### 4-10. 마감 후 투표 불가

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감시간 이후 투표 불가 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `Vote.validateCanVote()` - `isExpired()` 체크. UI에서 `th:unless="${expired}"`로 투표 폼 숨김 |

#### Playwright 시나리오

```
TC-VOTE-10: 마감된 투표에서 투표 폼 미표시 및 결과 표시
사전조건: 마감시간이 지난 투표
1. GET /votes/{id} 접속
2. "마감" 배지 표시 확인
3. 투표 폼(체크박스, "투표하기" 버튼) 미표시 확인
4. "투표 결과" 섹션 표시 확인
5. 각 후보지별 득표수 표시 확인
```

---

### 4-11. 마감 후 1등 표시 (공동 1등 지원)

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감 후 1등 표시 (공동 1등 지원) |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `Vote.getWinners()` - 최대 득표수를 구하고, 해당 득표수를 가진 모든 항목 반환 (공동 1등 지원). 득표가 0이면 빈 리스트 반환 |
| **비고** | `vote/detail.html`에서 `winners` 목록을 "1등" 배지와 함께 강조 표시 |

#### Playwright 시나리오

```
TC-VOTE-11: 마감 후 단독 1등 표시
사전조건: A=3표, B=1표, C=0표인 마감된 투표
1. GET /votes/{id} 접속
2. "투표 결과" 섹션에서 "1등" 배지 + "A식당" + "3표" 표시 확인
3. "1등" 배지가 1개만 존재 확인

TC-VOTE-12: 마감 후 공동 1등 표시
사전조건: A=2표, B=2표, C=1표인 마감된 투표
1. GET /votes/{id} 접속
2. "투표 결과" 섹션에서 "1등" 배지가 2개 표시 확인
3. "A식당" + "2표", "B식당" + "2표" 모두 1등으로 강조 표시 확인

TC-VOTE-13: 마감 후 모두 0표일 때 1등 미표시
사전조건: 아무도 투표하지 않은 마감된 투표
1. GET /votes/{id} 접속
2. "투표 결과" 섹션에서 "1등" 배지 미표시 확인
3. 전체 결과 목록에서 모든 항목 "0표" 표시 확인
```

---

### 4-12. 투표 생성 유효성 검증

| 항목 | 내용 |
|------|------|
| **요구사항** | 투표 제목, 마감시간 필수 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `CreateVoteRequest` - `@NotBlank` 제목/yearMonth/deadline, `@Min(1)` maxSelections |

#### Playwright 시나리오

```
TC-VOTE-14: 투표 제목 미입력 시 생성 불가
1. input[name="title"] 비워둠
2. deadline, maxSelections 정상 입력
3. "투표 만들기" 클릭
4. HTML required validation 또는 서버 에러 확인

TC-VOTE-15: 마감시간 미입력 시 생성 불가
1. title 정상 입력
2. input[name="deadline"] 비워둠
3. "투표 만들기" 클릭
4. HTML required validation 또는 서버 에러 확인
```

---

## 5. 날짜 투표

### 5-1. 날짜 선택 캘린더 표시

| 항목 | 내용 |
|------|------|
| **요구사항** | 하나의 투표에 장소 + 가능 날짜를 모두 선택 / 해당 월의 달력 표시 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteController.detail()` - `YearMonth.parse(vote.yearMonth)`로 해당 월의 날짜 리스트 생성 (`calendarDates`). `vote/detail.html`에서 7열 그리드 캘린더 렌더링 |
| **비고** | 요일 헤더: 일/월/화/수/목/금/토. 첫 날 이전 빈 칸 패딩 처리. 각 날짜에 체크박스 |

#### Playwright 시나리오

```
TC-DATE-01: 투표 상세 페이지에서 달력 표시
사전조건: 2026-03 월 투표 생성, 진행중 상태
1. GET /votes/{id} 접속
2. "가능한 날짜 선택" 섹션 확인
3. 요일 헤더 7개 (일~토) 표시 확인
4. 2026년 3월 1일~31일까지 날짜 표시 확인
5. 3월 1일이 일요일이면 첫 칸에 위치 확인 (요일 정렬)
```

---

### 5-2. 날짜 투표 수행

| 항목 | 내용 |
|------|------|
| **요구사항** | 가능 날짜를 선택 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | 폼에서 `selectedDates` 체크박스(값: LocalDate 문자열). `VoteService.castBallot()` - `selectedDates` 처리, `VoteDateBallot` 저장 |

#### Playwright 시나리오

```
TC-DATE-02: 날짜 투표 수행
사전조건: 진행중인 2026-03 투표
1. 장소 체크박스 1개 선택
2. 날짜 캘린더에서 3월 15일, 3월 20일 클릭 (체크박스 선택)
3. "투표하기" 클릭
4. 페이지 새로고침 후 3월 15일, 20일이 선택 상태(파란색 배경) 확인
5. 선택한 날짜에 투표 인원 수 배지 표시 확인 (1명)
```

---

### 5-3. 장소 + 날짜 동시 투표

| 항목 | 내용 |
|------|------|
| **요구사항** | 하나의 투표에 장소 + 가능 날짜를 모두 선택 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteController.castBallot()` - `selectedItems`(장소)와 `selectedDates`(날짜) 모두 수신. `VoteService.castBallot()` - 장소/날짜 각각 delete + save |

#### Playwright 시나리오

```
TC-DATE-03: 장소와 날짜를 동시에 투표
1. "A식당" 체크
2. 3월 10일, 3월 17일 날짜 체크
3. "투표하기" 클릭
4. 장소: A식당 체크 상태 유지, 날짜: 10일/17일 선택 상태 유지 확인
```

---

### 5-4. 날짜 투표 변경

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감 전 투표 변경 가능 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `castBallot()`에서 기존 날짜 투표 삭제 후 재저장: `voteDateBallotRepository.deleteAllByVoteIdAndVoterId()` |

#### Playwright 시나리오

```
TC-DATE-04: 날짜 투표 변경
사전조건: 3월 15일, 20일에 이미 투표한 상태
1. 15일 체크 해제, 25일 체크 선택
2. "투표하기" 클릭
3. 20일, 25일만 선택 상태 확인
4. 15일 인원 수 배지 사라짐 확인
```

---

### 5-5. 날짜별 가능 인원 카운트 표시

| 항목 | 내용 |
|------|------|
| **요구사항** | (암묵적) 날짜별 투표 현황 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `VoteService.getDateBallotSummary()` - `groupBy { it.availableDate }` 후 count. 캘린더에서 각 날짜의 투표 수를 배지로 표시 (`dateSummary`) |

#### Playwright 시나리오

```
TC-DATE-05: 여러 사용자 투표 후 날짜별 카운트 확인
1. 유저A: 3월 15일, 20일 투표
2. 유저B: 3월 15일, 25일 투표
3. 투표 상세에서 확인:
   - 3월 15일 배지: "2"
   - 3월 20일 배지: "1"
   - 3월 25일 배지: "1"
   - 다른 날짜는 배지 미표시
```

---

### 5-6. 마감 후 날짜별 결과 표시

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감 후 결과 표시 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `vote/detail.html` - `th:if="${expired && !dateSummary.isEmpty()}"` 조건으로 "날짜별 가능 인원" 섹션 표시. 날짜 포맷: `MM/dd (E)` |

#### Playwright 시나리오

```
TC-DATE-06: 마감 후 날짜별 가능 인원 결과 표시
사전조건: 날짜 투표가 있는 마감된 투표
1. GET /votes/{id} 접속
2. "날짜별 가능 인원" 섹션 표시 확인
3. 각 날짜(MM/dd 형식) + "N명" 포맷 확인
```

---

### 5-7. 장소 투표 없이 날짜만 투표

| 항목 | 내용 |
|------|------|
| **요구사항** | (에지 케이스) |
| **구현 상태** | 구현 가능 |
| **관련 코드** | `VoteController.castBallot()` - `selectedItems`는 `required = false`, null이면 빈 리스트. 날짜만 선택해도 서버 에러 없음 |

#### Playwright 시나리오

```
TC-DATE-07: 장소 미선택, 날짜만 투표
1. 장소 체크박스 모두 미선택
2. 3월 15일만 날짜 선택
3. "투표하기" 클릭
4. 정상 처리 확인 (에러 없음)
5. 장소 득표: 모두 0표, 날짜 15일 배지 "1" 확인
```

---

### 5-8. 날짜 미선택, 장소만 투표

| 항목 | 내용 |
|------|------|
| **요구사항** | (에지 케이스) |
| **구현 상태** | 구현 가능 |
| **관련 코드** | `selectedDates`는 `required = false`, null이면 빈 리스트 |

#### Playwright 시나리오

```
TC-DATE-08: 날짜 미선택, 장소만 투표
1. "A식당" 체크
2. 날짜는 모두 미선택
3. "투표하기" 클릭
4. 정상 처리 확인
5. A식당 1표, 날짜 배지 없음 확인
```

---

## 6. UI / UX

### 6-1. 토스 스타일 디자인

| 항목 | 내용 |
|------|------|
| **요구사항** | 토스 스타일, 데스크톱 우선 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | TailwindCSS 사용. 토스 컬러 팔레트(`#3182f6` primary). 둥근 모서리(`rounded-2xl`), 카드형 UI |
| **비고** | 모든 템플릿에 `max-w-4xl mx-auto` 적용하여 데스크톱 중심 레이아웃 |

#### Playwright 시나리오

```
TC-UI-01: 토스 스타일 시각 확인 (수동 QA 또는 스크린샷 비교)
1. /login 페이지 스크린샷 촬영
2. /restaurants 페이지 스크린샷 촬영
3. /votes 페이지 스크린샷 촬영
4. /votes/{id} 상세 페이지 스크린샷 촬영
5. primary 색상 (#3182f6) 사용 여부 확인
6. 카드형 라운드 UI (rounded-2xl) 확인
```

---

### 6-2. Thymeleaf + HTMX + TailwindCSS 사용

| 항목 | 내용 |
|------|------|
| **요구사항** | Thymeleaf + HTMX + TailwindCSS |
| **구현 상태** | 구현 완료 |
| **관련 코드** | Thymeleaf: 모든 `.html` 템플릿. HTMX: 후보지 등록 폼(`hx-post`, `hx-target`, `hx-swap`). TailwindCSS: CDN `https://cdn.tailwindcss.com` |
| **비고** | HTMX는 후보지 등록에서만 사용 (부분 갱신). 투표 관련은 전통적 form submit + redirect 방식 |

#### Playwright 시나리오

```
TC-UI-02: HTMX 부분 갱신 동작 확인 (후보지 등록)
1. /restaurants 접속
2. 후보지 등록 폼 입력 후 "등록" 클릭
3. 페이지 전체 새로고침 없이 목록만 갱신됨 확인 (네트워크 탭에서 HTMX 요청 확인)
4. 등록 폼이 리셋됨 확인
```

---

### 6-3. 데스크톱 우선 레이아웃

| 항목 | 내용 |
|------|------|
| **요구사항** | 데스크톱 우선 |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `max-w-4xl mx-auto` (최대 너비 896px 중앙 정렬) |
| **비고** | viewport meta 태그 포함으로 모바일에서도 접근 가능하지만 최적화는 데스크톱 기준 |

#### Playwright 시나리오

```
TC-UI-03: 데스크톱 해상도에서 레이아웃 확인
1. 브라우저 크기 1280x720 설정
2. /restaurants 접속
3. 컨텐츠가 중앙 정렬되고 좌우 여백 존재 확인
4. 후보지 카드, 등록 폼 정상 배치 확인
```

---

### 6-4. 스티키 헤더

| 항목 | 내용 |
|------|------|
| **요구사항** | (암묵적 UX) |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `layout/default.html` - `header` 태그에 `sticky top-0 z-50` 클래스 |

#### Playwright 시나리오

```
TC-UI-04: 스크롤 시 헤더 고정 확인
1. 후보지가 많은 상태에서 스크롤 다운
2. 헤더(팀 서비스 + 탭)가 상단에 고정되어 있는지 확인
```

---

## 7. 에러 처리

### 7-1. 전역 에러 페이지

| 항목 | 내용 |
|------|------|
| **요구사항** | (인프라) |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `GlobalExceptionHandler` - `CoreException` 캐치 후 `error.html` 렌더링. 에러 페이지에 "돌아가기" 버튼(`history.back()`) |

#### Playwright 시나리오

```
TC-ERR-01: 존재하지 않는 투표 접근 시 에러 페이지
1. GET /votes/999999 접속 (존재하지 않는 ID)
2. "오류가 발생했습니다" 제목 표시 확인
3. "투표를 찾을 수 없습니다" 메시지 표시 확인
4. "돌아가기" 버튼 존재 확인
```

---

### 7-2. 마감된 투표에 API 직접 투표 시도

| 항목 | 내용 |
|------|------|
| **요구사항** | 마감 후 투표 불가 |
| **구현 상태** | 구현 완료 (서버 측 검증) |
| **관련 코드** | `Vote.validateCanVote()` -> `CoreException(ErrorType.VOTE_EXPIRED)` |

#### Playwright 시나리오

```
TC-ERR-02: 마감된 투표에 POST 직접 전송 시 에러
사전조건: 마감된 투표 ID=X
1. POST /votes/X/ballot 직접 요청 (selectedItems=1)
2. 에러 페이지에 "투표가 마감되었습니다" 메시지 확인
```

---

## 8. 보안 / 세션

### 8-1. 세션 타임아웃

| 항목 | 내용 |
|------|------|
| **요구사항** | (인프라) |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `application.properties` - `server.servlet.session.timeout=24h` |

#### Playwright 시나리오

```
TC-SEC-01: (수동 확인) 세션 설정 확인
- application.properties에서 server.servlet.session.timeout=24h 확인
- 24시간 미활동 시 세션 만료 -> 로그인 페이지 리다이렉트 확인
```

---

### 8-2. 비밀번호 암호화 저장

| 항목 | 내용 |
|------|------|
| **요구사항** | (보안) |
| **구현 상태** | 구현 완료 |
| **관련 코드** | `PasswordEncoderConfig` - `BCryptPasswordEncoder` 사용. `AuthService.loginOrRegister()`에서 `passwordEncoder.encode()` / `passwordEncoder.matches()` |

---

## 요구사항 대비 구현 상태 요약

| # | 요구사항 | 구현 상태 | 비고 |
|---|---------|----------|------|
| 1 | 이름 + 비밀번호(숫자 4자리) 로그인 | 구현 완료 | `@Pattern("^[0-9]{4}$")` 서버/클라이언트 모두 검증 |
| 2 | 없는 이름이면 계정 생성 | 구현 완료 | `AuthService.loginOrRegister()` |
| 3 | 있는 이름이면 비밀번호 확인 | 구현 완료 | BCrypt 비교 |
| 4 | 비밀번호 불일치 시 에러 메시지 | 구현 완료 | "비밀번호가 일치하지 않습니다" |
| 5 | 탭 네비게이션 (후보지, 투표) | 구현 완료 | `activeTab` 변수로 활성 탭 표시 |
| 6 | 회식투표 탭이 default | 구현 완료 | `/ -> redirect:/restaurants` (후보지가 default) |
| 7 | 월별 여러 후보지 등록 | 구현 완료 | yearMonth 기반 조회 |
| 8 | 후보지 필드: 가게이름, 음식종류, Link, 등록자명 | 구현 완료 | 등록자명은 세션에서 자동 추출 |
| 9 | 누구나 등록, 누구나 투표 | 구현 완료 | 별도 권한 체크 없음 |
| 10 | 투표 생성 시 해당 월 후보지 스냅샷 | 구현 완료 | `VoteItem`으로 복사 저장 |
| 11 | 최대 선택 가능 수 설정 | 구현 완료 | 서버 검증만 (클라이언트 JS 미구현) |
| 12 | 투표 생성 이후 추가 후보지 미포함 | 구현 완료 | `VoteItem` 스냅샷 방식 |
| 13 | 마감시간 설정 | 구현 완료 | `datetime-local` 입력, `LocalDateTime` 저장 |
| 14 | 마감 전 투표 변경 | 구현 완료 | delete + save 방식 |
| 15 | 마감 후 1등 표시 (공동 1등) | 구현 완료 | `Vote.getWinners()` |
| 16 | 한 달에 여러 투표 | 구현 완료 | 유니크 제약 없음 |
| 17 | 장소 + 날짜 동시 선택 | 구현 완료 | 하나의 폼에서 `selectedItems` + `selectedDates` |
| 18 | 해당 월 달력 표시 | 구현 완료 | 7열 그리드 캘린더, 요일 헤더 |
| 19 | 토스 스타일 UI | 구현 완료 | TailwindCSS + 토스 컬러 |
| 20 | 데스크톱 우선 | 구현 완료 | `max-w-4xl` 레이아웃 |
| 21 | Thymeleaf + HTMX + TailwindCSS | 구현 완료 | HTMX는 후보지 등록에서만 사용 |

---

## 발견된 개선 포인트 (QA/개발자 참고)

### P1 - 기능 관련

| # | 항목 | 설명 | 우선순위 |
|---|------|------|---------|
| 1 | 클라이언트 측 최대 선택 수 제한 없음 | 서버에서만 `maxSelections` 검증. JS로 체크박스 선택 수 제한 필요 | 중 |
| 2 | 후보지 등록 시 HTMX 에러 핸들링 | `RestaurantController.register()`에서 `BindingResult` 미사용. 유효성 실패 시 사용자에게 친절한 에러 미표시 가능 | 중 |
| 3 | 투표 생성 시 후보지 0개인 경우 | 후보지가 없는 월에서 투표 생성 시 `voteItems`가 빈 리스트인 투표 생성됨. 사전 검증 없음 | 중 |
| 4 | 투표 목록에서 상세로의 뒤로가기 | 투표 상세에서 목록으로 돌아가는 명시적 네비게이션(뒤로가기 버튼) 없음 (탭 클릭으로 가능하지만 yearMonth 파라미터 유실 가능) | 하 |

### P2 - 보안 관련

| # | 항목 | 설명 | 우선순위 |
|---|------|------|---------|
| 1 | CSRF 보호 | 별도 CSRF 토큰 없음. Spring Security 미사용 (PasswordEncoder만 Bean 등록). form POST 요청에 CSRF 보호 고려 필요 | 중 |
| 2 | 이름 중복 시 악용 가능 | 이름만으로 계정 식별. 동일 이름 선점 가능 | 하 |

### P3 - UX 관련

| # | 항목 | 설명 | 우선순위 |
|---|------|------|---------|
| 1 | 투표 결과 실시간 갱신 | `result-fragment` HTMX 엔드포인트 존재하지만 `detail.html`에서 폴링/자동 갱신 미사용 | 하 |
| 2 | 후보지 삭제 기능 미구현 | 등록만 가능하고 삭제/수정 불가 | 하 |
| 3 | 투표 삭제/수정 기능 미구현 | 투표 생성 후 삭제/수정 불가 | 하 |

---

## 테스트 환경 구성 가이드 (Playwright)

### 사전 준비

```bash
# 1. MySQL Docker 실행 (3307 포트)
docker-compose -f docker/infra-compose.yml up -d

# 2. DB 스키마 생성 (ddl-auto=none이므로 수동 필요)
# DDL 스크립트를 DB에 직접 적용

# 3. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 4. Base URL: http://localhost:8080
```

### 테스트 데이터 초기화 전략

- 각 테스트 시나리오 전에 DB를 초기 상태로 리셋하거나, 고유한 이름/제목을 사용하여 테스트 간 격리
- 계정 생성은 `/login` POST로 자동 처리 (별도 API 불필요)
- 마감된 투표 테스트: 과거 시간의 deadline으로 투표를 생성하거나, DB 직접 조작

### 주요 셀렉터 참고

| 요소 | 셀렉터 |
|------|--------|
| 이름 입력 (로그인) | `input[name="name"]` |
| 비밀번호 입력 (로그인) | `input[name="password"]` |
| 시작하기 버튼 | `button[type="submit"]` (로그인 페이지) |
| 에러 메시지 | `.bg-red-50.text-red-600` |
| 후보지 탭 | `a[href="/restaurants"]` |
| 투표 탭 | `a[href="/votes"]` |
| 가게이름 입력 | `input[name="name"]` (레스토랑 폼) |
| 음식종류 입력 | `input[name="foodType"]` |
| 링크 입력 | `input[name="link"]` |
| 등록 버튼 | `form[hx-post="/restaurants"] button[type="submit"]` |
| 후보지 목록 컨테이너 | `#restaurant-list` |
| 투표 제목 입력 | `input[name="title"]` |
| 마감시간 입력 | `input[name="deadline"]` |
| 최대 선택 수 입력 | `input[name="maxSelections"]` |
| 투표 만들기 버튼 | `form[action="/votes"] button[type="submit"]` |
| 장소 체크박스 | `input[name="selectedItems"]` |
| 날짜 체크박스 | `input[name="selectedDates"]` |
| 투표하기 버튼 | `form[action*="/ballot"] button[type="submit"]` |
| 1등 배지 | `.text-primary.font-bold` (텍스트: "1등") |
| 진행중 배지 | `.bg-primary\\/10.text-primary` (텍스트: "진행중") |
| 마감 배지 | `.bg-gray-100.text-gray-500` (텍스트: "마감") |
| 로그아웃 버튼 | `form[action="/logout"] button` |
| 사용자 이름 (헤더) | `header span.text-gray-600` |
| 월 이전 버튼 | 월 선택기 내 첫 번째 `a` 태그 |
| 월 다음 버튼 | 월 선택기 내 두 번째 `a` 태그 |
