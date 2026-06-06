// ui/analysis/ReportItem.java
package com.example.plog.ui.analysis;

import com.example.plog.model.MonthlyReport;

public abstract class ReportItem {

    public static final int TYPE_HEADER  = 0;
    public static final int TYPE_SUMMARY = 1;
    public static final int TYPE_PLACE   = 2;
    public static final int TYPE_LABEL   = 3;
    public static final int TYPE_RADIUS  = 4;

    public abstract int getType();

    public static class HeaderItem extends ReportItem {
        public final String title;
        public HeaderItem(String title) { this.title = title; }
        @Override public int getType() { return TYPE_HEADER; }
    }

    public static class SummaryItem extends ReportItem {
        public final String summaryText;
        public final int    totalCount;
        public final int    activeDays;
        public SummaryItem(String text, int total, int days) {
            this.summaryText = text;
            this.totalCount  = total;
            this.activeDays  = days;
        }
        @Override public int getType() { return TYPE_SUMMARY; }
    }

    public static class PlaceItem extends ReportItem {
        public final int                    rank;
        public final MonthlyReport.PlaceItem data;
        public PlaceItem(int rank, MonthlyReport.PlaceItem data) {
            this.rank = rank; this.data = data;
        }
        @Override public int getType() { return TYPE_PLACE; }
    }

    public static class LabelItem extends ReportItem {
        public final int                    rank;
        public final MonthlyReport.LabelItem data;
        public LabelItem(int rank, MonthlyReport.LabelItem data) {
            this.rank = rank; this.data = data;
        }
        @Override public int getType() { return TYPE_LABEL; }
    }

    public static class RadiusItem extends ReportItem {
        public final MonthlyReport.ActivityRadius data;
        public RadiusItem(MonthlyReport.ActivityRadius data) { this.data = data; }
        @Override public int getType() { return TYPE_RADIUS; }
    }
}