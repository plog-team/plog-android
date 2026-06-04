// model/MonthlyReport.java
package com.example.plog.model;

import java.util.List;

public class MonthlyReport {

    public final int year;
    public final int month;

    public final int totalPhotoCount;
    public final int activeDayCount;
    public final int uniqueLabelCount;

    public final List<PlaceItem>   topPlaces;
    public final List<LabelItem>   topLabels;
    public final ActivityRadius    activityRadius;
    public final String            summaryText;

    private MonthlyReport(Builder b) {
        this.year             = b.year;
        this.month            = b.month;
        this.totalPhotoCount  = b.totalPhotoCount;
        this.activeDayCount   = b.activeDayCount;
        this.uniqueLabelCount = b.uniqueLabelCount;
        this.topPlaces        = b.topPlaces;
        this.topLabels        = b.topLabels;
        this.activityRadius   = b.activityRadius;
        this.summaryText      = b.summaryText;
    }

    // ── TOP3 장소 ──────────────────────────────────────────────────────────
    public static class PlaceItem {
        public final double clusterLat;
        public final double clusterLng;
        public final int    visitCount;
        public final long   firstVisit;
        public final long   lastVisit;
        public final String placeLabel;    // "1위 장소", "2위 장소" …
        public final String locationName;  // 역지오코딩 결과 (없으면 null)

        public PlaceItem(double lat, double lng, int count,
                         long first, long last, String label, String locationName) {
            this.clusterLat   = lat;
            this.clusterLng   = lng;
            this.visitCount   = count;
            this.firstVisit   = first;
            this.lastVisit    = last;
            this.placeLabel   = label;
            this.locationName = locationName;
        }
    }

    // ── TOP3 라벨 ──────────────────────────────────────────────────────────
    public static class LabelItem {
        public final String labelText;
        public final int    frequency;
        public final float  avgConfidence;

        public LabelItem(String text, int freq, float avg) {
            this.labelText     = text;
            this.frequency     = freq;
            this.avgConfidence = avg;
        }

        public String getConfidencePercent() {
            return String.format("%.0f%%", avgConfidence * 100);
        }
    }

    // ── 활동 반경 ──────────────────────────────────────────────────────────
    public static class ActivityRadius {
        public final double centerLat;
        public final double centerLng;
        public final double radiusMeters;
        public final double totalDistanceMeters;

        public ActivityRadius(double lat, double lng,
                              double radius, double totalDist) {
            this.centerLat           = lat;
            this.centerLng           = lng;
            this.radiusMeters        = radius;
            this.totalDistanceMeters = totalDist;
        }

        public String getRadiusText() {
            if (radiusMeters >= 1000)
                return String.format("%.1f km", radiusMeters / 1000);
            return String.format("%.0f m", radiusMeters);
        }

        public String getTotalDistanceText() {
            if (totalDistanceMeters >= 1000)
                return String.format("%.1f km", totalDistanceMeters / 1000);
            return String.format("%.0f m", totalDistanceMeters);
        }
    }

    // ── Builder ────────────────────────────────────────────────────────────
    public static class Builder {
        int year, month, totalPhotoCount, activeDayCount, uniqueLabelCount;
        List<PlaceItem>  topPlaces;
        List<LabelItem>  topLabels;
        ActivityRadius   activityRadius;
        String           summaryText;

        public Builder year(int v)                     { year             = v; return this; }
        public Builder month(int v)                    { month            = v; return this; }
        public Builder totalPhotoCount(int v)          { totalPhotoCount  = v; return this; }
        public Builder activeDayCount(int v)           { activeDayCount   = v; return this; }
        public Builder uniqueLabelCount(int v)         { uniqueLabelCount = v; return this; }
        public Builder topPlaces(List<PlaceItem> v)    { topPlaces        = v; return this; }
        public Builder topLabels(List<LabelItem> v)    { topLabels        = v; return this; }
        public Builder activityRadius(ActivityRadius v){ activityRadius   = v; return this; }
        public Builder summaryText(String v)           { summaryText      = v; return this; }
        public MonthlyReport build()                   { return new MonthlyReport(this); }
    }
}