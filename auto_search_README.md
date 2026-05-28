# plog-android [feature/auto-fill-search] — 현재 코드 구조/환경 가이드 (merge용)

Constants 값 정책

`app/src/main/java/com/example/plog/util/Constants.java`는 저장소에 커밋되는 파일이므로,
원격 브랜치에는 아래 정책을 유지합니다.

- `KAKAO_REST_API_KEY = ""`
- `OPENWEATHER_API_KEY = ""`
- `KAKAO_COORD_TO_ADDRESS_URL` / `OPENWEATHER_CURRENT_WEATHER_URL` : 공식 API URL 고정값 사용 가능
- `BASE_URL` : 로컬 개발 환경에 맞게 개별 설정 필요 (기본값 빈 문자열 권장) |
---

> 이 문서는 **코드 구현 기준**으로 필요한 구조/설치요소/실행환경까지 포함한 가이드입니다.
> (화면 코드 + Gradle/Manifest/Wrapper 설정 파일을 함께 기준으로 정리)

---


## 0) 코드 기준 필수 설치/설정 파일

아래 파일들이 있어야 **동일한 코드가 동일하게 빌드/실행**됩니다.

| 분류 | 파일 | 역할 |
| --- | --- | --- |
| Gradle 실행기 | `gradle/wrapper/gradle-wrapper.properties` + `gradle/wrapper/gradle-wrapper.jar` | Gradle 9.3.1 고정(팀원 환경 차이 최소화) |
| 버전 카탈로그 | `gradle/libs.versions.toml` | AGP/AndroidX 라이브러리 버전 소스 |
| 루트 빌드 설정 | `settings.gradle`, `build.gradle`, `gradle.properties` | 저장소/플러그인/전역 JVM 옵션 관리 |
| 앱 모듈 빌드 설정 | `app/build.gradle` | compileSdk/targetSdk, Java 17, 의존성 선언 |
| 앱 권한/네트워크 | `app/src/main/AndroidManifest.xml` | INTERNET, 미디어 권한, cleartext(개발용) |
| 로컬 SDK 경로(개인 파일) | `local.properties` | Android SDK 경로 지정(커밋 대상 아님) |

### 로컬에 설치되어야 하는 도구
- **Android Studio** (Gradle Sync/AVD 관리)
- **JDK 17** (`app/build.gradle`의 source/targetCompatibility 기준)
- **Android SDK**: 최소 `minSdk 26`, 타겟/컴파일 `36`
- **ADB** (APK 설치/실행 확인용)

### 코드가 사용하는 핵심 라이브러리(설치 자동)
`app/build.gradle` + `libs.versions.toml` 기준으로 Gradle Sync 시 자동 설치됩니다.
- Retrofit 2.9.0 / Gson Converter 2.9.0
- OkHttp 4.12.0 / Logging Interceptor 4.12.0
- Navigation Fragment/UI 2.7.7
- Glide 4.16.0
- ExifInterface 1.3.7

---"

## 1) 현재 화면 흐름

```
┌──────────────────────────────────────────────────┐
│ MainActivity                                     │
│ ─ NavHostFragment + BottomNavigation 연결         │
│ ─ destination 변경 시 상단 타이틀 변경             │
└───────────────────────┬──────────────────────────┘
                        │
                        ▼
       ┌──────────────────────────────────────────┐
       │ nav_graph.xml                            │
       │ startDestination: searchFragment (현재)   │
       │ destinations:                             │
       │ - homeFragment                            │
       │ - recommendFragment                       │
       │ - localFragment                           │
       │ - searchFragment                          │
       │ - myFragment                              │
       │ - autoFillFragment                        │
       └───────────────┬──────────────────────────┘
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
┌──────────────────────┐   ┌──────────────────────────┐
│ SearchFragment       │   │ AutoFillFragment         │
│ - 서버 일기 조회      │   │ - 이미지 선택             │
│ - 키워드 검색         │   │ - EXIF 날짜/좌표 추출      │
│ - RecyclerView 출력   │   │ - Kakao 주소 변환          │
│                      │   │ - OpenWeather 날씨 조회    │
└──────────────────────┘   └──────────────────────────┘
```

---

## 2) 네트워크/상수 정책

| 항목 | 현재 구현 |
| --- | --- |
| BASE_URL | `Constants.BASE_URL = http://10.0.2.2:8080/` |
| 검색 API | `GET /api/diaries/search` (`SearchFragment`에서 OkHttp 직접 호출) |
| 주소 API | Kakao coord2address (REST Key 필요) |
| 날씨 API | OpenWeather current weather (API Key 필요) |
| Retrofit | `ApiClient` 싱글톤 존재, `ApiService`는 현재 비어 있음 |
| 로깅 | OkHttp LoggingInterceptor BODY 레벨 (`ApiClient`) |

### 민감정보/공개정보 구분
- **커밋 금지**: API Key, private 서버 주소.
- **커밋 가능**: 공식 공개 API endpoint URL (예: Kakao/OpenWeather URL).

---

## 3) 핵심 파일 트리 (코드 기준)

```
app/src/main/java/com/example/plog/
├── MainActivity.java
├── util/
│   └── Constants.java                # [외부환경] BASE_URL/Kakao/OpenWeather 키·URL 상수 관리
├── network/
│   ├── ApiClient.java                # [외부환경] Retrofit baseUrl=Constants.BASE_URL, OkHttp 로깅
│   └── ApiService.java
├── autofill/
│   ├── AutoFillFragment.java         # [자동입력 핵심] EXIF + Kakao 주소변환 API + OpenWeather API 호출
│   └── AutoFillData.java
├── search/
│   ├── SearchFragment.java           # [검색 핵심] Spring 서버(BASE_URL) /api/diaries/search 호출
│   ├── SearchDiary.java
│   └── SearchDiaryAdapter.java
└── ui/home/
    └── HomeFragment.java

app/src/main/res/
├── navigation/
│   └── nav_graph.xml                 # [외부환경 연동 흐름] 검색/자동입력 화면 진입 경로
└── layout/
    ├── activity_main.xml
    ├── fragment_search.xml
    ├── fragment_auto_fill.xml
    ├── fragment_home.xml
    └── item_search_diary.xml
```

> 자동입력/검색 관련 외부 연동 요약
> - 자동입력(`AutoFillFragment`): **Kakao Local API**, **OpenWeather API**, EXIF 위치/시간 정보 사용
> - 검색(`SearchFragment`): **Spring Boot 서버 API**(`BASE_URL + api/diaries/search`) 사용
> - 공통 설정(`Constants.java`): 위 API들의 **키/URL/BASE_URL** 중앙 관리


---


## 3-1) 핵심 파일트리 + 연결 환경/API 매핑

| 파일 | 역할 | 연결 환경/API | 비고 |
| --- | --- | --- | --- |
| `util/Constants.java` | 서버/외부 API 상수 중앙 관리 | `BASE_URL`, `KAKAO_REST_API_KEY`, `OPENWEATHER_API_KEY`, `KAKAO_COORD_TO_ADDRESS_URL`, `OPENWEATHER_CURRENT_WEATHER_URL` | 민감값(API Key)은 빈 값 커밋, 공식 endpoint URL은 커밋 가능 |
| `search/SearchFragment.java` | 일기 목록 조회 + 키워드 검색 | `BASE_URL + api/diaries/search` (Spring Boot) | OkHttp GET 요청, 검색어는 URL 인코딩 |
| `autofill/AutoFillFragment.java` | 사진 EXIF 기반 자동입력 | Kakao Map(Local) coord2address API, OpenWeather current weather API | `Constants` 키/URL 사용, 키 미설정 시 API 호출 스킵 |
| `network/ApiClient.java` | Retrofit/OkHttp 공통 클라이언트 | `Constants.BASE_URL` | BODY 로그 인터셉터 포함(개발용) |
| `network/ApiService.java` | Retrofit API 인터페이스 | (현재 메서드 비어 있음) | 향후 Search/AutoFill API 추상화 가능 |

### Constants.java 상세 설명
- `BASE_URL`: 앱 내부 서버 통신 기본 주소 (`http://10.0.2.2:8080/` 형태).
- `KAKAO_REST_API_KEY`: Kakao Local API Authorization 헤더(`KakaoAK ...`)에 사용.
- `OPENWEATHER_API_KEY`: OpenWeather 요청 쿼리 파라미터(`appid`)에 사용.
- `KAKAO_COORD_TO_ADDRESS_URL`: 위도/경도를 주소로 바꾸는 Kakao endpoint.
- `OPENWEATHER_CURRENT_WEATHER_URL`: 위도/경도 기준 현재 날씨 endpoint.

> 권장: 팀 공용 문서에는 키 값 자체를 쓰지 말고, 어떤 필드에 어떤 용도로 들어가는지만 관리합니다.

---

## 4) 권한/플랫폼 포인트

| 항목 | 설명 |
| --- | --- |
| `INTERNET` | 서버/Kakao/OpenWeather 호출 필요 |
| `ACCESS_MEDIA_LOCATION` | EXIF 위치 접근 권한 요청 (Android Q+) |
| 이미지 선택 | ACTION_PICK + ACTION_OPEN_DOCUMENT 지원 |
| SAF | `ACTION_OPEN_DOCUMENT` 경로는 `takePersistableUriPermission` 처리 |

---

## 5) 기능별 동작 메모

### SearchFragment
- 초기 진입 시 전체 목록 요청.
- 검색 버튼/키보드 검색 액션으로 키워드 검색.
- 검색어 비우면 전체 목록 재조회.
- `BASE_URL` 미설정 시 요청 중단 + 로그 출력.

### AutoFillFragment
- 사진 선택 후 EXIF 날짜 파싱.
- EXIF GPS가 있으면:
  - Kakao API로 주소 문자열 변환
  - OpenWeather API로 현재 날씨 변환
- 키 미설정 시 각 API 호출을 안전하게 스킵.

---

## 6) 빌드/실행 체크

### 로컬 준비
- Android Studio SDK 설정 (`local.properties`의 `sdk.dir` 자동 생성)
- 백엔드 실행 (`http://10.0.2.2:8080/` 접근 가능해야 함)

### 빌드
```bash
./gradlew assembleDebug
```

### 실행
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.plog/.MainActivity
```

### 연동 확인
1. 백엔드 검색 API: `GET /api/diaries/search`
2. 앱 검색 탭에서 리스트 로딩 확인
3. 자동입력 탭에서 사진 선택 후 날짜/위치/날씨 확인

---

## 7) merge 전 최종 체크리스트

- [ ] `Constants.java`에 실제 API Key가 남아있지 않은가?
- [ ] `BASE_URL`이 팀 공용 개발 주소인가?
- [ ] Kakao/OpenWeather 공식 URL이 정확한가?
- [ ] `nav_graph.xml`의 startDestination이 의도된 화면인가?
- [ ] 검색/자동입력 탭에서 크래시 없이 기본 동작이 되는가?

---

## 8) AI 가이드 파트 문서화 가이드 (추가 예정)

현재 브랜치에는 `ui/aiguide/*` 코드가 아직 없습니다.  
AI 가이드 파트를 병합할 때는 아래를 같은 형식으로 추가하면 됩니다.

- 화면 흐름 (Entry → Session/Chat → Draft)
- DTO/ApiResponse 구조
- Retrofit API 메서드 목록
- nav_graph destination + 단독 진입점
- 권한/네트워크/타임아웃 정책
- 시연용 startDestination 변경/원복 절차