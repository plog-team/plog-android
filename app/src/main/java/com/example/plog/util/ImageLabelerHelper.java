// util/ImageLabelerHelper.java
package com.example.plog.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.plog.model.LabelResult;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.List;

public class ImageLabelerHelper {

    private static final String TAG = "ImageLabelerHelper";

    /**
     * confidence threshold 권장값
     *
     * 0.50 → 범용 일반 사진 분류 (기본값, 노이즈 적음)
     * 0.65 → 고신뢰 라벨만 표시 (결과 수 줄어들지만 정확도 ↑)
     * 0.40 → 최대한 많은 라벨 수집 (노이즈 증가 주의)
     *
     * 일기 앱 특성상 0.65 권장:
     *   - "산책", "카페", "야경" 등 의미 있는 라벨 위주로 필터링
     *   - 불필요한 "Rectangle", "Font" 같은 저신뢰 라벨 제거
     */
    public static final float THRESHOLD_DEFAULT = 0.65f;
    public static final float THRESHOLD_LOOSE   = 0.40f;  // 최대 수집용
    public static final float THRESHOLD_STRICT  = 0.80f;  // 고정밀 필터

    /**
     * 비동기 라벨링 콜백 인터페이스
     */
    public interface LabelingCallback {
        void onSuccess(@NonNull List<LabelResult> labels);
        void onFailure(@NonNull Exception e);
    }

    private final ImageLabeler labeler;

    /**
     * @param confidenceThreshold 최소 신뢰도 (0.0 ~ 1.0)
     *                            이 값 미만의 라벨은 결과에서 제외됨
     */
    public ImageLabelerHelper(float confidenceThreshold) {
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(confidenceThreshold)
                .build();

        // close()가 필요 없는 싱글톤 패턴 — 앱 생명주기와 함께 유지
        this.labeler = ImageLabeling.getClient(options);
    }

    /**
     * URI로부터 이미지 라벨링 실행 (비동기 — ML Kit Task API 사용)
     *
     * @param uri      갤러리 URI
     * @param context  컨텍스트
     * @param callback 결과 콜백
     */
    public void analyze(@NonNull Uri uri,
                        @NonNull Context context,
                        @NonNull LabelingCallback callback) {

        InputImage image = ImageInputFactory.fromUri(uri, context);
        if (image == null) {
            callback.onFailure(new IllegalArgumentException("InputImage 생성 실패: " + uri));
            return;
        }

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    // ImageLabel → LabelResult 변환
                    List<LabelResult> results = new ArrayList<>();
                    for (ImageLabel label : labels) {
                        results.add(new LabelResult(
                                label.getText(),
                                label.getConfidence(),
                                label.getIndex()
                        ));
                    }
                    Log.d(TAG, "라벨링 완료 — " + results.size() + "개 결과");
                    callback.onSuccess(results);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "라벨링 실패", e);
                    callback.onFailure(e);
                });
    }

    /**
     * ViewModel 소멸 시 호출하여 리소스 해제
     */
    public void close() {
        labeler.close();
    }
}