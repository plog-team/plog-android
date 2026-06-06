package com.example.plog.data;

public class DiaryEmojiDecoration {
    private String id;
    private String diaryDate;
    private String authorName;
    private String emoji;
    private float xRatio;
    private float yRatio;
    private float scale;
    private long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDiaryDate() {
        return diaryDate;
    }

    public void setDiaryDate(String diaryDate) {
        this.diaryDate = diaryDate;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public float getXRatio() {
        return xRatio;
    }

    public void setXRatio(float xRatio) {
        this.xRatio = xRatio;
    }

    public float getYRatio() {
        return yRatio;
    }

    public void setYRatio(float yRatio) {
        this.yRatio = yRatio;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
