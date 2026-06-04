package com.example.plog.data;

import java.util.ArrayList;
import java.util.List;

public class DiaryEntry {
    private String date;
    private String title;
    private String body;
    private String location;
    private String weather;
    private boolean secret;
    private boolean bookmarked;
    private int representativePhotoIndex;
    private List<String> photoUris;
    /** photo 테이블 image_url 에 저장된 갤러리 URI — 일기 수정 시 DB 정합성 유지에 사용 */
    private List<String> galleryPhotoUris;

    public DiaryEntry() {
        photoUris = new ArrayList<>();
        galleryPhotoUris = new ArrayList<>();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public List<String> getPhotoUris() {
        return photoUris;
    }

    public void setPhotoUris(List<String> photoUris) {
        this.photoUris = photoUris == null ? new ArrayList<>() : photoUris;
    }

    public List<String> getGalleryPhotoUris() {
        return galleryPhotoUris;
    }

    public void setGalleryPhotoUris(List<String> galleryPhotoUris) {
        this.galleryPhotoUris = galleryPhotoUris == null ? new ArrayList<>() : galleryPhotoUris;
    }

    public String getRepresentativePhotoUri() {
        if (photoUris == null || photoUris.isEmpty()) {
            return null;
        }
        int safeIndex = Math.max(0, Math.min(representativePhotoIndex, photoUris.size() - 1));
        return photoUris.get(safeIndex);
    }

    public int getRepresentativePhotoIndex() {
        return representativePhotoIndex;
    }

    public void setRepresentativePhotoIndex(int representativePhotoIndex) {
        this.representativePhotoIndex = Math.max(0, representativePhotoIndex);
    }
}
