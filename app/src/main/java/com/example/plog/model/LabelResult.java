// model/LabelResult.java
package com.example.plog.model;

import androidx.annotation.NonNull;

/**
 * ML Kit 라벨링 결과 단위
 * RecyclerView Adapter 및 Room 저장에 공통 사용
 */
public class LabelResult {

    private final String text;          // 라벨 텍스트  (예: "Dog", "Sunset")
    private final float  confidence;    // 신뢰도 0.0 ~ 1.0
    private final int    index;         // ML Kit 내부 분류 인덱스

    public LabelResult(String text, float confidence, int index) {
        this.text       = text;
        this.confidence = confidence;
        this.index      = index;
    }

    public String getText()       { return text; }
    public float  getConfidence() { return confidence; }
    public int    getIndex()      { return index; }

    /** confidence를 퍼센트 문자열로 반환 (예: "87.3%") */
    public String getConfidencePercent() {
        return String.format("%.1f%%", confidence * 100);
    }

    @NonNull
    @Override
    public String toString() {
        return text + " (" + getConfidencePercent() + ")";
    }
}