# plog-android — 자동입력 및 검색 파트 정리

> 아래 내용은 기존 문서와 분리된 복사용 신규 문서입니다.

> 사진을 선택하면 촬영일·위치·날씨를 일기 작성 화면에 자동 반영하고, 저장된 일기를 키워드와 정렬 조건으로 조회하는 파트입니다.
> 이 문서는 **현재 Android 코드에서 확인되는 요청·응답 계약과 전체 연결 구조**를 기준으로 정리합니다.

---

## 한눈에 보는 전체 구조

현재 자동입력 기능에는 목적이 다른 두 경로가 공존합니다.

| 구분 | 실제 연결 방식 | 용도 |
| --- | --- | --- |
| **일기 작성 자동입력** | Android → Spring Boot 사진 업로드 → `photoId` 기반 자동입력 API | 실제 `DiaryEditFragment`에서 대표사진의 날짜·위치·날씨 자동 반영 |
| **별도 자동입력 화면** | Android에서 EXIF 직접 추출 → Kakao/OpenWeather 직접 호출 | `AutoFillFragment` 단독 화면/기존 구현 |
| **일기 검색** | Android → Spring Boot 검색 API | 키워드 검색, 최신순·오래된순 정렬, 이미지 포함 목록 표시 |

즉, 이번에 일기 작성 화면에 연결된 핵심 자동입력은 `AutoFillFragment`를 거치지 않습니다.
> `DiaryEditFragment → PhotoViewModel → PhotoRepository → ApiService → Backend` 순서로 사진을 먼저 업로드한 뒤, 백엔드가 돌려준 `serverPhotoId`로 자동입력 정보를 다시 조회합니다.

---

## 화면 흐름

```text
┌──────────────────────────────────────────────────────────────┐
│ DiaryEditFragment — 실제 일기 작성/수정 화면                  │
│ ─ 사진 선택: ACTION_OPEN_DOCUMENT, 최대 10장                  │
│ ─ 선택 사진을 로컬 Room DB에 저장                             │
│ ─ 각 사진을 백엔드 POST /api/photos 로 비동기 업로드           │
│ ─ 첫 사진 또는 사용자가 지정한 대표사진으로 자동입력 실행       │
└───────────────────────────┬──────────────────────────────────┘
                            │ galleryUri로 Room DB 조회
                            │ server_photo_id 업로드 완료 대기
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ GET /api/photos/{photoId}/auto-input                         │
│ ─ date → 일기 날짜 표시                                      │
│ ─ locationHint → 위치 입력칸                                 │
│ ─ weather + temperature → 날씨 입력칸                        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│ SearchFragment — 일기 검색 화면                              │
│ ─ 최초 진입/검색어 삭제: 전체 목록 조회                       │
│ ─ 검색 버튼/키보드 검색: keyword 조회                         │
│ ─ 최신순 ↔ 오래된순 토글: sort 변경 후 재조회                  │
│ ─ data 배열을 SearchDiary 목록으로 변환                       │
│ ─ SearchDiaryAdapter에서 이미지 URL을 Glide로 표시            │
└──────────────────────────────────────────────────────────────┘
```

별도의 `AutoFillFragment`에서는 사진 한 장을 선택한 뒤 단말에서 EXIF 날짜·GPS를 읽고, GPS가 있으면 Kakao 주소 API와 OpenWeather API를 앱에서 직접 호출합니다. 이 화면은 백엔드 `photoId` 기반 자동입력 흐름과 독립적입니다.

---

## 네트워크 계층

| 항목 | 현재 구현 |
| --- | --- |
| 백엔드 기본 주소 | `Constants.BASE_URL` (`http://10.0.2.2:8080/`, 에뮬레이터 → 호스트 PC) |
| Retrofit/OkHttp | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson |
| 공통 인증 헤더 | Retrofit 요청은 `UserIdInterceptor`가 `X-User-Id: 1` 자동 부착 |
| Retrofit 타임아웃 | connect 15초 / read·write 90초 |
| Retrofit 로깅 | BODY 레벨 |
| 공통 백엔드 응답 | `ApiResponse<T> = { success, data, error }` |
| 자동입력 API 호출 | `ApiService` + 동기 `execute()`를 별도 스레드에서 실행 |
| 검색 API 호출 | `SearchFragment`의 별도 `OkHttpClient`로 직접 GET 요청 |
| 검색 인증 헤더 | Retrofit 인터셉터를 거치지 않으므로 `X-User-Id`를 요청에 직접 추가 |
| 외부 주소 API | Kakao Local `coord2address` — `AutoFillFragment`에서만 직접 호출 |
| 외부 날씨 API | OpenWeather current weather — `AutoFillFragment`에서만 직접 호출 |

### 중요: 검색과 자동입력의 네트워크 방식 차이

- 사진 업로드와 `photoId` 자동입력 조회는 `ApiClient.getApiService()`를 사용하므로 공통 헤더·Gson 변환·타임아웃 설정이 적용됩니다.
- 검색은 Retrofit을 사용하지 않고 `new OkHttpClient()`로 직접 호출합니다. 따라서 헤더 추가, URL 조립, JSON 파싱을 `SearchFragment`가 직접 담당합니다.
- 검색 응답도 백엔드 공통 래퍼 형태이지만 DTO/Gson으로 변환하지 않고 `JSONObject`에서 루트의 `data` 배열을 꺼냅니다.

---

## 핵심 파일 트리

```text
app/src/main/java/com/example/plog/
├── util/
│   └── Constants.java                       BASE_URL, API URL/키, 개발용 userId
├── network/
│   ├── ApiClient.java                       Retrofit 싱글톤 + UserIdInterceptor
│   ├── ApiService.java                      사진 업로드/삭제/자동입력 API 선언
│   └── UserIdInterceptor.java               X-User-Id 공통 부착
├── model/
│   ├── ApiResponse.java                     공통 래퍼 {success, data, error}
│   ├── PhotoUploadBatchResponse.java        업로드 응답 data.photos
│   ├── PhotoUploadResponse.java             업로드된 사진 메타데이터 + photoId
│   └── PhotoAutoInputContext.java           자동입력 응답 data
├── data/
│   ├── repository/PhotoRepository.java      로컬 저장, 서버 업로드, serverPhotoId 저장
│   ├── db/dao/PhotoDao.java                 galleryUri ↔ server_photo_id 조회/갱신
│   └── db/entity/PhotoEntity.java           Room photo 테이블
├── ui/
│   ├── diary/DiaryEditFragment.java         대표사진 선택 + 백엔드 자동입력 반영
│   └── photo/PhotoViewModel.java             Fragment와 PhotoRepository 연결
├── search/
│   ├── SearchFragment.java                  검색 요청 URL/헤더/응답 파싱
│   ├── SearchDiary.java                     검색 결과 UI 모델
│   └── SearchDiaryAdapter.java              검색 카드 바인딩 + Glide 이미지 로딩
└── autofill/
    ├── AutoFillFragment.java                EXIF + 외부 API 직접 호출 별도 화면
    └── AutoFillData.java                    별도 자동입력 화면 표시 모델

app/src/main/res/
├── layout/fragment_diary_edit.xml            실제 일기 작성 화면
├── layout/fragment_search.xml                검색 화면
├── layout/item_search_diary.xml              검색 결과 카드
├── layout/fragment_auto_fill.xml             별도 자동입력 화면
└── navigation/nav_graph.xml                  searchFragment / autoFillFragment 진입점
```

---

## 백엔드 연결 API 및 반환 구조

### 1. 사진 업로드

```http
POST /api/photos
X-User-Id: 1
Content-Type: multipart/form-data

file=<선택한 사진 바이너리>
```

Android의 `PhotoRepository`가 갤러리 URI를 바이트 배열로 읽고 multipart의 `file` 파트로 업로드합니다. 응답의 첫 번째 `photoId`를 Room DB의 `photo.server_photo_id`에 저장합니다.

```json
{
  "success": true,
  "data": {
    "photos": [
      {
        "photoId": 12,
        "sha256": "...",
        "originalFilename": "photo_...jpg",
        "mimeType": "image/jpeg",
        "width": 3024,
        "height": 4032,
        "sizeBytes": 1234567,
        "storedPath": "/...",
        "cacheHit": false
      }
    ]
  },
  "error": null
}
```

Android 접근 경로:

```text
response.body().data.photos.get(0).photoId
  → PhotoDao.updateServerPhotoId(galleryUri, serverPhotoId)
```

### 2. 사진 자동입력 정보 조회

```http
GET /api/photos/{photoId}/auto-input
X-User-Id: 1
```

```json
{
  "success": true,
  "data": {
    "photoId": 12,
    "capturedAt": "2026-06-04T13:20:00",
    "date": "2026년 6월 4일",
    "latitude": 37.5665,
    "longitude": 126.9780,
    "locationHint": "서울특별시 중구",
    "weather": "맑음",
    "temperature": 24.3
  },
  "error": null
}
```

| 응답 필드 | Android 반영 위치 | null/빈 값 처리 |
| --- | --- | --- |
| `date` | `DiaryEditFragment.tvDate` | `날짜 정보 없음` |
| `locationHint` | `DiaryEditFragment.etLocation` | `위치 정보 없음` |
| `weather` | `DiaryEditFragment.etWeather` | `날씨 정보 없음` |
| `temperature` | 날씨 뒤에 ` / {temperature}℃` 추가 | 값이 없으면 날씨만 표시 |
| `photoId`, `capturedAt`, `latitude`, `longitude` | DTO에는 저장되지만 현재 화면에 직접 표시하지 않음 | 별도 처리 없음 |

### 3. 일기 검색

```http
GET /api/diaries/search?sort=latest&keyword={URL-encoded keyword}
X-User-Id: 1
```

- `keyword`가 비어 있으면 쿼리에서 생략합니다.
- `sort`는 `latest` 또는 `oldest`입니다.
- 검색창이 비워지면 전체 목록을 다시 요청합니다.

```json
{
  "success": true,
  "data": [
    {
      "diary_date": "2026-06-04",
      "emotion": "행복",
      "title": "한강 산책",
      "content": "날씨가 좋아서 산책했다.",
      "location_name": "여의도 한강공원",
      "image_url": "/uploads/diary/12.jpg"
    }
  ],
  "error": null
}
```

검색 파서는 백엔드 필드명이 바뀌거나 혼재한 경우를 고려해 아래 대체 키를 허용합니다.

| `SearchDiary` 필드 | 우선 키 | 대체 키 |
| --- | --- | --- |
| 날짜 | `diary_date` | `date` |
| 감정 | `emotion` | 빈 문자열 |
| 제목 | `title` | 빈 문자열 |
| 내용 | `content` | `body` |
| 위치 | `location_name` | `location` |
| 이미지 | `image_url` | `imageUrl` |

이미지 경로가 `http`로 시작하면 그대로 사용하고, 상대 경로이면 `Constants.BASE_URL`을 앞에 붙여 Glide로 로딩합니다. 이미지가 없거나 로딩에 실패하면 placeholder를 표시합니다.

> 위 JSON은 Android DTO와 파싱 코드가 기대하는 **응답 계약 예시**입니다. 백엔드 내부의 실제 분석·검색 구현 세부사항은 이 저장소에 포함되어 있지 않습니다.

---

## 시퀀스 다이어그램 — 일기 작성 자동입력

```text
DiaryEditFragment     PhotoViewModel      PhotoRepository      Room DB          Backend
       │                    │                    │                │                │
       │ 사진 선택(최대 10장) │                    │                │                │
       ├─ processPhoto(uri) ─▶│                    │                │                │
       │                    ├─ savePhoto(uri) ──▶│                │                │
       │                    │                    ├─ EXIF 추출 ───▶│ photo/location 저장
       │                    │                    ├─ POST /api/photos ─────────────▶│
       │                    │                    │                │◀─ photoId ─────│
       │                    │                    ├─ server_photo_id 저장 ─────────▶│
       │                    │                    │                │                │
       │ 대표사진 자동입력 시작│                    │                │                │
       ├─ galleryUri로 serverPhotoId 조회(0.5초 간격, 최대 10초) ─▶│                │
       │◀────────────────────────────── serverPhotoId ───────────│                │
       ├─ GET /api/photos/{photoId}/auto-input ─────────────────────────────────▶│
       │◀─ ApiResponse<PhotoAutoInputContext> ───────────────────────────────────│
       ├─ date/locationHint/weather/temperature UI 반영                           │
       │                    │                    │                │                │
       │ 대표사진 변경       │                    │                │                │
       └─ 선택된 대표사진 URI 기준으로 같은 자동입력 흐름 재실행                   │
```

### 자동입력 실패 처리

- 업로드 후 10초 안에 `server_photo_id`가 Room DB에 기록되지 않으면, “사진 분석 중입니다. 잠시 후 대표사진을 다시 선택해주세요.”를 표시합니다.
- 자동입력 API가 성공 응답을 주지 않거나 `data`가 없으면, “자동입력 정보를 불러오지 못했습니다.”를 표시합니다.
- 사진 업로드 실패는 로컬 사진 저장을 취소하지 않고 로그만 남깁니다.
- 자동입력 API 호출은 별도 스레드에서 동기 실행하며, UI 갱신은 메인 스레드에서 수행합니다.

---

## 시퀀스 다이어그램 — 검색

```text
User                 SearchFragment             Backend              SearchDiaryAdapter
 │                          │                       │                         │
 │ 검색 화면 진입            │                       │                         │
 ├─────────────────────────▶│ GET /api/diaries/search?sort=latest          │
 │                          ├──────────────────────▶│                         │
 │                          │◀─ {success,data[],error}                       │
 │                          ├─ data 배열 수동 파싱                           │
 │                          └───────────────────────────────────────────────▶│ 목록/이미지 표시
 │                          │                       │                         │
 │ 키워드 입력 + 검색         │                       │                         │
 ├─────────────────────────▶│ keyword URL 인코딩                             │
 │                          ├─ GET ...?sort=latest&keyword=... ────────────▶│
 │                          │◀─ 검색 결과 data[] ──────────────────────────│
 │                          └───────────────────────────────────────────────▶│ 목록 교체
 │                          │                       │                         │
 │ 최신순/오래된순 토글       │                       │                         │
 ├─────────────────────────▶│ sort 값 변경 후 현재 키워드로 재조회           │
```

---

## 별도 `AutoFillFragment` 흐름

```text
AutoFillFragment
  ├─ 갤러리 또는 파일에서 사진 1장 선택
  ├─ ExifInterface로 촬영일과 GPS 추출
  ├─ GPS 있음
  │   ├─ Kakao coord2address API → 주소 표시
  │   └─ OpenWeather current weather API → 현재 날씨/기온 표시
  └─ GPS 없음 → 위치 정보 없음, 외부 API 호출 생략
```

이 경로의 특징과 제한사항:

- Spring Boot 백엔드를 거치지 않습니다.
- 주소·날씨 API 키가 앱 상수에 필요합니다.
- OpenWeather 호출은 사진 촬영 당시 날씨가 아니라 해당 좌표의 **현재 날씨**를 조회합니다.
- 실제 일기 작성 화면의 백엔드 자동입력과 결과가 다를 수 있습니다.

---

## 권한 및 플랫폼 설정

| 권한/설정 | 사유 |
| --- | --- |
| `INTERNET` | 백엔드, Kakao, OpenWeather API 호출 |
| `ACCESS_MEDIA_LOCATION` | Android 10+에서 사진 원본 위치 메타데이터 접근 |
| `READ_MEDIA_IMAGES` | Android 13+ 이미지 접근 |
| `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`) | Android 12 이하 호환 |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | 위치 관련 기능 지원 |
| `usesCleartextTraffic="true"` | 개발 중 `http://10.0.2.2:8080/` 통신 허용 |

일기 작성 사진 선택은 `ACTION_OPEN_DOCUMENT`를 사용하며 URI 읽기 권한을 유지합니다. 배포 환경에서는 HTTPS 적용 후 cleartext 허용을 제거해야 합니다.

---

## 빌드 및 연동 확인

### 필수 설정

- Android Studio / JDK 17 / Android SDK 설치
- `local.properties`에 Android SDK 경로 설정
- 백엔드를 호스트 PC의 8080 포트에서 실행
- 에뮬레이터에서는 `10.0.2.2`가 호스트 PC를 가리킴
- 외부 API 직접 호출 화면을 사용할 경우 Kakao/OpenWeather 키를 로컬 개발 환경에 안전하게 설정

### 빌드

```bash
./gradlew assembleDebug
```

### 에뮬레이터 설치 및 실행

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.plog/.MainActivity
```

### 자동입력 확인 순서

1. 백엔드를 실행합니다.
2. 일기 작성 화면에서 EXIF 정보가 있는 사진을 선택합니다.
3. 사진 업로드 후 날짜·위치·날씨가 자동 반영되는지 확인합니다.
4. 대표사진을 바꾸었을 때 선택한 대표사진 기준으로 자동입력이 다시 수행되는지 확인합니다.
5. 업로드/자동입력 요청에 `X-User-Id`가 포함되는지 확인합니다.

### 검색 확인 순서

1. 검색 화면 진입 시 전체 일기 목록이 표시되는지 확인합니다.
2. 검색 버튼과 키보드 검색 액션 모두 동작하는지 확인합니다.
3. 검색어를 지우면 전체 목록을 다시 조회하는지 확인합니다.
4. 최신순·오래된순 토글 시 `sort` 쿼리와 목록 순서가 변경되는지 확인합니다.
5. 상대/절대 이미지 URL과 이미지 없는 결과가 정상 표시되는지 확인합니다.

---

## 현재 구현 시 주의사항

- **자동입력은 업로드 완료 이벤트를 직접 받지 않고 Room DB를 0.5초 간격으로 최대 10초 polling**합니다. 네트워크가 느리면 대표사진을 다시 선택해야 할 수 있습니다.
- `DiaryEditFragment`의 자동입력 기준은 첫 선택 사진 또는 현재 대표사진입니다. 여러 사진의 정보를 병합하지 않습니다.
- 검색은 Retrofit 공통 클라이언트를 사용하지 않으므로 공통 타임아웃·로깅·인터셉터 정책이 자동 적용되지 않습니다.
- 검색 실패 시 현재 구현은 로그만 남기며 사용자용 오류/빈 결과 UI는 별도로 표시하지 않습니다.
- 검색 응답은 반드시 루트 객체 안에 `data` 배열이 있어야 합니다. 배열만 단독 반환하면 파싱되지 않습니다.
- `AutoFillFragment`의 직접 외부 API 방식과 `DiaryEditFragment`의 백엔드 자동입력 방식은 서로 독립적이므로, 향후 하나의 방식으로 통합할지 결정이 필요합니다.
- API 키와 비공개 서버 주소는 저장소에 커밋하지 않고 로컬 설정 또는 안전한 주입 방식으로 관리해야 합니다.