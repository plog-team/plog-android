# Changelog



---

## 2026-06-04
## local.properties에 MAPS_API_KEY=" " 를 추가 구글 지도 api
### Added

#### 역지오코딩 (위도/경도 → 장소명 자동 변환)
- `PhotoRepository.savePhoto()` — GPS 저장 직후 Android `Geocoder`로 역지오코딩 실행
- `location_name` 우선순위: POI명(숫자 번지 제외) → 동 → 시 → 도
- `AnalysisViewModel.loadReport()` — 기존 DB에 `location_name`이 없는 항목 일괄 backfill
- `PhotoLocationDao.getMissingLocationNames()` — backfill 대상 쿼리 (삭제된 사진 제외)

#### 일기 사진 수정 → DB 반영
- `DiaryEditFragment` — `activeGalleryUris` 추적으로 사진 교체 시 기존 사진 소프트 삭제
- `DiaryEntry` — `galleryPhotoUris` 필드 추가 (갤러리 URI ↔ 로컬 파일 URI 매핑)
- `DiaryRepository` — `galleryPhotoUris` 직렬화/역직렬화
- `PhotoDao.softDeleteByImageUrl()` — image_url 기준 소프트 삭제
- `PhotoViewModel.removePhotoByUrl()` — ViewModel에서 삭제 호출

#### 분석 리포트 장소명 표시
- `PhotoLocationDao.getTopLocationClusters()` — `MAX(location_name)` 쿼리에 추가
- `MonthlyReport.PlaceItem` — `locationName` 필드 추가
- `ReportAdapter.PlaceVH` — `locationName` 있으면 장소명, 없으면 좌표 폴백

#### 서버 연동
- `PhotoRepository.uploadToServer()` — 사진 저장 시 `POST api/photos` 자동 업로드 (fire-and-forget)
- `ApiService.updatePreferences()` — `PUT api/preferences` 선호도 동기화 엔드포인트
- `PreferenceUpdateRequest` — 선호도 요청 DTO (`preferredCategories` 콤마 구분 문자열)
- `AnalysisRepository.syncPreferencesToServer()` — 선호도 저장 후 서버 자동 동기화
- `ApiService.deletePhoto()` — `DELETE api/photos/{photoId}` 사진 삭제 엔드포인트
- `PhotoEntity` — `server_photo_id` 컬럼 추가 (업로드 후 서버 ID 저장)
- `PhotoDao.getServerPhotoIdByImageUrl()` / `updateServerPhotoId()` — serverPhotoId 조회·저장 쿼리
- `PhotoRepository.softDeleteByImageUrl()` — 로컬 소프트 삭제 + 서버 삭제 API 동시 호출
- `AppDatabase` — version 3으로 업그레이드 (server_photo_id 컬럼 추가 반영)

#### 사용자 선호도 점수 개선 (EMA)
- `UserPreferenceScoreDao` — `getLastUpdated()`, `decayAll()` 추가
- `AnalysisRepository.savePreferenceScores()` — 단순 덮어쓰기 → EMA 방식으로 교체
  - 새 달 진입 시: 기존 점수 전체 × 0.7 decay → TOP3 EMA 블렌딩 (70% 신규 + 30% 기존)
  - 같은 달 재조회: 현재 분석 결과로 업데이트 (decay 없음)

### Changed

- `ReportAdapter` — "자주 찾은 장소 TOP3" → "사진 많이 찍은 장소 TOP3"
- `ReportAdapter` — "N회 방문" → "사진 N장" (visitCount의 실제 의미 반영)
- `AnalysisRepository.generateSummary()` — "가장 자주 찾은 곳" → "사진을 가장 많이 찍은 곳"

### Fixed

- `PhotoLocationDao` 전체 쿼리에 `AND p.is_deleted = 0` 누락 수정
  - 대상: `getAllWithLocationLive`, `getAllWithLocationAndImage`, `getLocationsInRangeSync`,
    `getTopLocationClusters`, `getMonthlyLocationsWithImage`
  - 효과: 일기에서 삭제된 사진이 리포트·지도에서도 제거됨

---

