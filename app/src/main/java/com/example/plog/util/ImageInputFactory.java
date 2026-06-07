package com.example.plog.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;

/**
 * ML Kit InputImage 생성 유틸
 *
 * ❌ InputImage.fromBitmap()  → 메모리 복사 발생, 대용량 이미지 OOM 위험
 * ✅ InputImage.fromFilePath() 또는 InputImage.fromContentUri() 사용
 *    → URI 직접 처리, 내부적으로 회전 보정(EXIF orientation) 자동 적용
 */
public class ImageInputFactory {

    private static final String TAG = "ImageInputFactory";

    /**
     * URI → InputImage 변환
     * EXIF 회전 정보를 자동으로 처리하므로 별도 Bitmap 회전 불필요
     *
     * @param uri     갤러리에서 선택한 이미지 URI
     * @param context ApplicationContext 권장
     * @return InputImage 또는 null (생성 실패 시)
     */
    @Nullable
    public static InputImage fromUri(@NonNull Uri uri, @NonNull Context context) {
        try {
            // fromFilePath 내부에서 ContentResolver를 통해 InputStream을 열고
            // EXIF orientation 태그를 읽어 자동 회전 보정
            return InputImage.fromFilePath(context, uri);
        } catch (IOException e) {
            Log.e(TAG, "InputImage 생성 실패 — URI: " + uri, e);
            return null;
        }
    }
}