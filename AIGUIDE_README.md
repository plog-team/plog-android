# plog-android — AI 가이드 파트

> plog 안드로이드 앱의 "이미지 기반 AI 가이드 작성" 화면. 김용진(YongJin04) 담당. 단독 진입점 제공 → 다른 파트 머지 무관.

---

## 화면 흐름

```
┌─────────────────────────────────────────────────────┐
│  AiGuideEntryFragment                               │
│  ─ 사진 선택 (SAF OpenMultipleDocuments, 최대 10장) │
│  ─ Glide 96dp 미리보기 RecyclerView                  │
│  ─ 모드 RadioGroup    ⚡ BATCH / 💬 CONVERSATION    │
│  ─ 페르소나 RadioGroup 기본/친절/감성/딱딱/위트       │
│  ─ "AI 가이드 시작" → 병렬 업로드 + createAiSession │
└──────────┬───────────────────┬──────────────────────┘
           │                   │
           │ mode=BATCH        │ mode=CONVERSATION
           ▼                   ▼
┌──────────────────────┐  ┌──────────────────────┐
│ AiGuideSessionFragment│  │ AiGuideChatFragment  │
│ 질문 5개 RecyclerView │  │ 채팅 RecyclerView     │
│ + EditText (selective)│  │ + 입력창 + 전송        │
│ "초안 만들기"          │  │ readyForDraft=true시  │
│                      │  │ "초안 만들기" 활성     │
└──────────┬───────────┘  └──────────┬───────────┘
           └──────────┬───────────────┘
                      ▼
       ┌────────────────────────────────┐
       │ AiGuideDraftFragment           │
       │ ─ 초안 EditText + 글자수        │
       │ ─ "일기에 반영" → confirm       │
       │ ─ 별점 RatingBar + 코멘트       │
       │ ─ "피드백 보내기"               │
       │   → 가이드 MD 자동 학습          │
       └────────────────────────────────┘
```

`nav_graph.xml`에 `aiGuideEntryFragment` destination 추가 (단독 진입점). 조연준 `feature/diary-write` 머지 시점에 `btnAiQuestion` 1줄 PR로 Navigation 연결.

---

## 네트워크 계층

| 항목 | 결정 |
| --- | --- |
| HTTP | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson |
| BASE_URL | `http://10.0.2.2:8080/` (에뮬레이터 → 호스트 PC) |
| 타임아웃 | connect 15s / read·write 90s (Gemini Vision 대비) |
| 인증 | `UserIdInterceptor`가 모든 요청에 `X-User-Id: 1` 자동 부착 |
| 응답 | 모든 응답 `Call<ApiResponse<T>>`, `body().data` 접근 |
| 로깅 | BODY 인터셉터 (개발 시) |

---

## 핵심 파일 트리

```
app/src/main/java/com/example/plog/
├── MainActivity.java
├── util/Constants.java                   BASE_URL, X-User-Id 헤더 키
├── network/
│   ├── ApiClient.java                    Retrofit 싱글톤
│   ├── ApiService.java                   ★ 9개 메서드 (8개 ApiResponse wrap)
│   └── UserIdInterceptor.java
├── model/                                Gson DTO 13개
│   ├── ApiResponse.java                  ★ 공통 래퍼 {success, data, error}
│   ├── PhotoUploadResponse.java
│   ├── CreateSessionRequest/Response.java (+ mode + persona)
│   ├── SessionDetailResponse.java
│   ├── GuideQuestionDto.java
│   ├── ChatMessageDto.java
│   ├── SendChatRequest/Response.java
│   ├── AnswerRequest.java
│   ├── DraftResponse.java
│   ├── FeedbackRequest.java
│   ├── PhotoAnalysisDto.java
│   ├── VisionResult.java                 (snake_case @SerializedName)
│   └── ExifResult.java
└── ui/aiguide/                           ★ 4 Fragment
    ├── AiGuideEntryFragment.java         picker + 모드/페르소나 + 업로드
    ├── AiGuideSessionFragment.java       BATCH Q&A
    ├── AiGuideChatFragment.java          CONVERSATION 대화
    ├── AiGuideDraftFragment.java         초안 + 반영 + 피드백
    ├── SelectedPhotoAdapter.java         가로 썸네일
    ├── QuestionAnswerAdapter.java        BATCH 답변 EditText
    └── ChatMessageAdapter.java           USER 오른쪽 / ASSISTANT 왼쪽

res/
├── layout/  fragment_ai_guide_entry/session/chat/draft.xml +
│            item_selected_photo, item_question_answer,
│            item_chat_user, item_chat_assistant
└── navigation/nav_graph.xml              aiGuide{Entry,Session,Chat,Draft}
```

---

## 권한 (AndroidManifest)

| 권한 | 사유 |
| --- | --- |
| `INTERNET` | 백엔드 API 호출 |
| `READ_MEDIA_IMAGES` (SDK 33+) | SAF picker 이외 갤러리 직접 접근 (예비) |
| `READ_EXTERNAL_STORAGE` (maxSdk 32) | SDK 32 이하 호환 |
| `usesCleartextTraffic="true"` | 개발 중 http 통신 (배포 시 https로 교체) |

SAF `OpenMultipleDocuments`는 **런타임 권한 불필요**.

---

## 시퀀스 다이어그램 (BATCH 모드 풀체인)

```
Entry              Session              Backend          Gemini
 │ 사진 3장 선택       │                   │                │
 │ "AI 가이드 시작"     │                   │                │
 ├──── upload x3 ─────────────────────────▶│                │
 │                  │                    │ SHA-256+resize │
 │◀── photoIds[3,4,5] ───────────────────│                │
 ├──── createAiSession(mode, persona) ───▶│                │
 │                  │                    ├─ vision 분석 ──▶│
 │                  │                    │◀── JSON ───────│
 │                  │                    ├─ 질문 5개 생성   │
 │◀── sessionId + questions[5] ──────────│                │
 │ navigate(session)│                    │                │
 │                  │ 질문 표시           │                │
 │                  │ 답변 입력 selective │                │
 │                  │ "초안 만들기"        │                │
 │                  ├─ answer x3 ────────▶│                │
 │                  ├─ POST /draft ──────▶│                │
 │                  │                    ├─ 페르소나+가이드 MD ─▶│
 │                  │                    │◀── 일기 초안 ──│
 │                  │◀── draft 200~400자 │                │
 │                  │ navigate(draft)    │                │
 │                  │                    │                │
 │                Draft                  │                │
 │                  │ "일기에 반영"        │                │
 │                  ├─ POST /confirm ────▶│ user_state_memory 갱신
 │                  │ 별점 + 코멘트       │                │
 │                  ├─ POST /feedback ───▶│ DiaryGuideService.appendFromFeedback
 │                  │                    ├─ refine MD ───▶│
 │                  │                    │◀── 20줄 MD ────│
 │                  │                    │ user_state_memory upsert
 │                  │◀── 204             │                │
```

---

## 빌드 + 실행

### 필수 설정 (한 번만)
- `gradle.properties`에 `android.overridePathCheck=true` (한글 경로 호환)
- `local.properties`에 `sdk.dir=...` (Android Studio가 자동 생성)

### 빌드
```bash
./gradlew assembleDebug
# BUILD SUCCESSFUL in 14s
# app/build/outputs/apk/debug/app-debug.apk (9.0 MB)
```

### 에뮬레이터에 설치 + 실행
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.plog/.MainActivity
```

### 백엔드 연결 확인
1. 백엔드 `plog-backend`에서 `./gradlew bootRun` (포트 8080)
2. 에뮬레이터에서 `http://10.0.2.2:8080/ping` 확인

---

## 단독 진입점

`nav_graph.xml`의 `startDestination`을 일시적으로 `aiGuideEntryFragment`로 변경하면 단독으로 AI 가이드 화면 진입 가능 (시연용). 조연준 머지 후 원복.

```xml
<navigation
    ...
    app:startDestination="@id/aiGuideEntryFragment">  <!-- 시연용 임시 -->
```

---

## 비고

- 모든 응답은 `ApiResponse<T>`로 wrap됨 → 콜백에서 `resp.body().data` 접근
- 무료 quota 보호를 위해 같은 사진 재호출 시 캐시 hit (1~5ms)
- 페르소나 선택은 세션 생성 시점에 고정 (재현성)
