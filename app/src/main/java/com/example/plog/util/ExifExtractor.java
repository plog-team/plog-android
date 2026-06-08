package com.example.plog.util;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExifExtractor {

    private static final String TAG = "ExifExtractor";

    // EXIF 날짜 포맷 (예: "2024:03:15 14:30:00")
    private static final String EXIF_DATE_FORMAT = "yyyy:MM:dd HH:mm:ss";

    // ── 추출 결과를 담는 불변 데이터 클래스 ──────────────────────────────────
    public static class ExifResult {
        /**
         * epoch milliseconds. EXIF 없으면 -1
         */
        public final long takenAtMs;
        /**
         * GPS 위도. 없으면 Double.NaN
         */
        public final double latitude;
        /**
         * GPS 경도. 없으면 Double.NaN
         */
        public final double longitude;
        /**
         * 이미지 가로 픽셀. 없으면 0
         */
        public final int imageWidth;
        /**
         * 이미지 세로 픽셀. 없으면 0
         */
        public final int imageHeight;

        public ExifResult(long takenAtMs, double latitude, double longitude,
                          int imageWidth, int imageHeight) {
            this.takenAtMs = takenAtMs;
            this.latitude = latitude;
            this.longitude = longitude;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        /**
         * GPS 데이터가 유효한지 확인
         */
        public boolean hasLocation() {
            return !Double.isNaN(latitude) && !Double.isNaN(longitude);
        }

        /**
         * 촬영 시간 데이터가 유효한지 확인
         */
        public boolean hasTakenAt() {
            return takenAtMs > 0;
        }

        /**
         * Logcat 출력
         */
        public void logAll() {
            Log.d("ExifResult", "──────────────────────────────");
            Log.d("ExifResult", "촬영 시간   : " + (hasTakenAt()
                    ? new Date(takenAtMs).toString() : "없음"));
            Log.d("ExifResult", "위도        : " + (hasLocation()
                    ? latitude : "없음"));
            Log.d("ExifResult", "경도        : " + (hasLocation()
                    ? longitude : "없음"));
            Log.d("ExifResult", "이미지 크기 : " + imageWidth + " × " + imageHeight);
            Log.d("ExifResult", "──────────────────────────────");
        }
    }

    // ── 메인 추출 메서드 ──────────────────────────────────────────────────────
    // openFileDescriptor() 사용: Android 10+ ContentResolver의 InputStream 경로는
    // GPS EXIF 태그를 제거하지만, FileDescriptor는 원본 파일을 직접 가리키므로
    // setRequireOriginal() 없이도 GPS를 읽을 수 있다.
    // Photo Picker URI(content://media/picker/...)는 RedactingFileDescriptor를 반환해
    // GPS를 제거하므로 MediaStore URI로 변환 후 추출한다.
    @Nullable
    public ExifResult extract(@NonNull Uri uri, @NonNull Context context) {
        uri = toMediaStoreUri(uri);
        Log.d(TAG, "extract() 호출 — URI: " + uri);
        try (ParcelFileDescriptor pfd =
                     context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) {
                Log.w(TAG, "FileDescriptor가 null입니다. URI: " + uri);
                return null;
            }

            ExifInterface exif = new ExifInterface(pfd.getFileDescriptor());

            long takenAtMs  = parseTakenAt(exif);
            double[] latLng = parseLatLng(exif);
            int width       = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
            int height      = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);

            ExifResult result = new ExifResult(takenAtMs, latLng[0], latLng[1], width, height);
            Log.d(TAG, "extract() 완료 — hasGPS=" + result.hasLocation()
                    + " lat=" + result.latitude + " lng=" + result.longitude);
            return result;

        } catch (Exception e) {
            // SecurityException(권한 없음) 포함 모든 예외를 null로 처리해 사진 저장은 계속 진행
            Log.e(TAG, "EXIF 추출 실패 (" + e.getClass().getSimpleName() + "): "
                    + e.getMessage() + " | URI: " + uri);
            return null;
        }
    }

    // ── 촬영 시간 파싱 ────────────────────────────────────────────────────────
    private long parseTakenAt(@NonNull ExifInterface exif) {
        String raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
        if (raw == null || raw.isBlank()) {
            raw = exif.getAttribute(ExifInterface.TAG_DATETIME);
        }
        if (raw == null || raw.isBlank()) {
            Log.d(TAG, "촬영 시간 태그 없음");
            return -1L;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(EXIF_DATE_FORMAT, Locale.US);
            Date date = sdf.parse(raw.trim());
            return (date != null) ? date.getTime() : -1L;
        } catch (ParseException e) {
            Log.w(TAG, "날짜 파싱 실패: " + raw, e);
            return -1L;
        }
    }

    @NonNull
    private double[] parseLatLng(@NonNull ExifInterface exif) {

        String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String latVal = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String lngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        String lngVal = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

        Log.d(TAG, "GPS RAW — latRef=" + latRef
                + " latVal=" + latVal
                + " lngRef=" + lngRef
                + " lngVal=" + lngVal);

        // latRef가 없으면 직접 수동 파싱 시도
        double[] latLng = exif.getLatLong();

        if (latLng == null) {
            // getLatLong() 실패 시 수동 파싱 시도
            latLng = parseLatLngManually(latVal, latRef, lngVal, lngRef);
        }

        if (latLng == null) {
            Log.d(TAG, "GPS 태그 없음");
            return new double[]{Double.NaN, Double.NaN};
        }

        double lat = latLng[0];
        double lng = latLng[1];

        Log.d(TAG, "GPS 파싱 결과 — lat=" + lat + " lng=" + lng);

        // 0,0 또는 범위 초과 제거
        if ((lat == 0.0 && lng == 0.0)
                || lat < -90.0 || lat > 90.0
                || lng < -180.0 || lng > 180.0) {
            Log.d(TAG, "GPS 유효하지 않음 — lat=" + lat + " lng=" + lng);
            return new double[]{Double.NaN, Double.NaN};
        }

        return new double[]{lat, lng};
    }

    /**
     * getLatLong() 실패 시 DMS 문자열을 직접 파싱
     * "37/1,30/1,0/1" → 37.5 형태로 변환
     */
    @Nullable
    private double[] parseLatLngManually(
            @Nullable String latVal, @Nullable String latRef,
            @Nullable String lngVal, @Nullable String lngRef) {

        if (latVal == null || lngVal == null) return null;

        try {
            double lat = parseDms(latVal);
            double lng = parseDms(lngVal);

            // S(남위), W(서경)이면 음수
            if ("S".equalsIgnoreCase(latRef)) lat = -lat;
            if ("W".equalsIgnoreCase(lngRef)) lng = -lng;

            Log.d(TAG, "수동 파싱 결과 — lat=" + lat + " lng=" + lng);
            return new double[]{lat, lng};

        } catch (Exception e) {
            Log.w(TAG, "수동 GPS 파싱 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * "37/1,30/1,15/100" → 37도 30분 0.15초 → decimal 변환
     */
    private double parseDms(@NonNull String dms) {
        String[] parts = dms.split(",");
        double degrees = parseRational(parts[0]);
        double minutes = parts.length > 1 ? parseRational(parts[1]) : 0;
        double seconds = parts.length > 2 ? parseRational(parts[2]) : 0;
        return degrees + minutes / 60.0 + seconds / 3600.0;
    }

    /**
     * "37/1" → 37.0,  "30/2" → 15.0
     */
    private double parseRational(@NonNull String rational) {
        String[] parts = rational.trim().split("/");
        if (parts.length == 2) {
            double numerator = Double.parseDouble(parts[0]);
            double denominator = Double.parseDouble(parts[1]);
            return denominator != 0 ? numerator / denominator : 0;
        }
        return Double.parseDouble(parts[0]);
    }

    /**
     * Photo Picker URI (content://media/picker/.../media/ID) →
     * MediaStore URI (content://media/external/images/media/ID) 변환.
     * Picker URI가 아니면 그대로 반환.
     */
    @NonNull
    private static Uri toMediaStoreUri(@NonNull Uri uri) {
        if (!"media".equals(uri.getAuthority())) return uri;
        java.util.List<String> segments = uri.getPathSegments();
        if (segments.isEmpty() || !"picker".equals(segments.get(0))) return uri;
        try {
            long mediaId = Long.parseLong(uri.getLastPathSegment());
            return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Picker URI ID 파싱 실패: " + uri.getLastPathSegment());
            return uri;
        }
    }
}