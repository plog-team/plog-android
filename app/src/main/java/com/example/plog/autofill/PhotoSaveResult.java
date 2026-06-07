package com.example.plog.autofill;

/** 업로드된 사진의 원본 URI와 DB photoId를 함께 저장하는 결과 객체 */
public class PhotoSaveResult {
    private final String uri;
    private final long photoId;

    public PhotoSaveResult(String uri, long photoId) {
        this.uri = uri;
        this.photoId = photoId;
    }

    public String getUri() {
        return uri;
    }

    public long getPhotoId() {
        return photoId;
    }
}
