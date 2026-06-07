// ui/analysis/ReportAdapter.java
package com.example.plog.ui.analysis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.model.MonthlyReport;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ReportItem> items = new ArrayList<>();

    public void submitReport(@NonNull MonthlyReport report) {
        items.clear();    // ← 기존 items 먼저 클리어

        // 1. 요약 카드
        items.add(new ReportItem.SummaryItem(
                report.summaryText,
                report.totalPhotoCount,
                report.activeDayCount
        ));

        // 2. TOP3 장소
        if (report.topPlaces != null && !report.topPlaces.isEmpty()) {
            items.add(new ReportItem.HeaderItem("사진 많이 찍은 장소 TOP3"));
            for (int i = 0; i < report.topPlaces.size(); i++) {
                items.add(new ReportItem.PlaceItem(i + 1, report.topPlaces.get(i)));
            }
        } else {
            // 데이터 없을 때도 헤더 + 안내 문구 표시
            items.add(new ReportItem.HeaderItem("사진 많이 찍은 장소 TOP3"));
            items.add(new ReportItem.HeaderItem("이번 달 GPS 데이터가 없어요."));
        }

        // 3. TOP3 라벨
        if (report.topLabels != null && !report.topLabels.isEmpty()) {
            items.add(new ReportItem.HeaderItem("자주 등장한 키워드 TOP3"));
            for (int i = 0; i < report.topLabels.size(); i++) {
                items.add(new ReportItem.LabelItem(i + 1, report.topLabels.get(i)));
            }
        } else {
            items.add(new ReportItem.HeaderItem("자주 등장한 키워드 TOP3"));
            items.add(new ReportItem.HeaderItem("이번 달 라벨 데이터가 없어요."));
        }

        // 4. 활동 반경
        if (report.activityRadius != null) {
            items.add(new ReportItem.HeaderItem("이번 달 활동 반경"));
            items.add(new ReportItem.RadiusItem(report.activityRadius));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        return switch (viewType) {
            case ReportItem.TYPE_HEADER  ->
                    new HeaderVH(inf.inflate(R.layout.item_report_header,  parent, false));
            case ReportItem.TYPE_SUMMARY ->
                    new SummaryVH(inf.inflate(R.layout.item_report_summary, parent, false));
            case ReportItem.TYPE_PLACE   ->
                    new PlaceVH(inf.inflate(R.layout.item_report_place,   parent, false));
            case ReportItem.TYPE_LABEL   ->
                    new LabelVH(inf.inflate(R.layout.item_report_label,   parent, false));
            case ReportItem.TYPE_RADIUS  ->
                    new RadiusVH(inf.inflate(R.layout.item_report_radius,  parent, false));
            default -> throw new IllegalStateException("Unknown viewType: " + viewType);
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ReportItem item = items.get(position);
        if      (holder instanceof HeaderVH  h) h.bind((ReportItem.HeaderItem)  item);
        else if (holder instanceof SummaryVH h) h.bind((ReportItem.SummaryItem) item);
        else if (holder instanceof PlaceVH   h) h.bind((ReportItem.PlaceItem)   item);
        else if (holder instanceof LabelVH   h) h.bind((ReportItem.LabelItem)   item);
        else if (holder instanceof RadiusVH  h) h.bind((ReportItem.RadiusItem)  item);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────────

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        HeaderVH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_section_title);
        }
        void bind(ReportItem.HeaderItem item) {
            tvTitle.setText(item.title);
        }
    }

    static class SummaryVH extends RecyclerView.ViewHolder {
        final TextView tvSummary, tvTotalCount, tvActiveDays;
        SummaryVH(@NonNull View v) {
            super(v);
            tvSummary    = v.findViewById(R.id.tv_summary);
            tvTotalCount = v.findViewById(R.id.tv_total_count);
            tvActiveDays = v.findViewById(R.id.tv_active_days);
        }
        void bind(ReportItem.SummaryItem item) {
            tvSummary.setText(item.summaryText);
            tvTotalCount.setText(item.totalCount + "장");
            tvActiveDays.setText(item.activeDays + "일");
        }
    }

    static class PlaceVH extends RecyclerView.ViewHolder {
        final TextView tvRank, tvLabel, tvVisitCount, tvCoord;
        PlaceVH(@NonNull View v) {
            super(v);
            tvRank       = v.findViewById(R.id.tv_rank);
            tvLabel      = v.findViewById(R.id.tv_place_label);
            tvVisitCount = v.findViewById(R.id.tv_visit_count);
            tvCoord      = v.findViewById(R.id.tv_coord);
        }
        void bind(ReportItem.PlaceItem item) {
            MonthlyReport.PlaceItem d = item.data;
            tvRank.setText(item.rank + "위");
            tvLabel.setText(d.placeLabel);
            tvVisitCount.setText("사진 " + d.visitCount + "장");
            if (d.locationName != null) {
                tvCoord.setText(d.locationName);
                tvCoord.setVisibility(View.VISIBLE);
            } else {
                tvCoord.setText(String.format("%.3f, %.3f", d.clusterLat, d.clusterLng));
                tvCoord.setVisibility(View.VISIBLE);
            }
        }
    }

    static class LabelVH extends RecyclerView.ViewHolder {
        final TextView    tvRank, tvText, tvFreq, tvConf;
        final ProgressBar pbConf;
        LabelVH(@NonNull View v) {
            super(v);
            tvRank  = v.findViewById(R.id.tv_rank);
            tvText  = v.findViewById(R.id.tv_label_text);
            tvFreq  = v.findViewById(R.id.tv_frequency);
            tvConf  = v.findViewById(R.id.tv_confidence);
            pbConf  = v.findViewById(R.id.pb_confidence);
        }
        void bind(ReportItem.LabelItem item) {
            MonthlyReport.LabelItem d = item.data;
            tvRank.setText(item.rank + "위");
            tvText.setText(d.labelText);
            tvFreq.setText(d.frequency + "회");
            tvConf.setText(d.getConfidencePercent());
            pbConf.setProgress(Math.round(d.avgConfidence * 100));
        }
    }

    static class RadiusVH extends RecyclerView.ViewHolder {
        final TextView tvRadius, tvDistance, tvCenter;
        RadiusVH(@NonNull View v) {
            super(v);
            tvRadius   = v.findViewById(R.id.tv_radius);
            tvDistance = v.findViewById(R.id.tv_total_distance);
            tvCenter   = v.findViewById(R.id.tv_center_coord);
        }
        void bind(ReportItem.RadiusItem item) {
            MonthlyReport.ActivityRadius d = item.data;
            tvRadius.setText("활동 반경: " + d.getRadiusText());
            tvDistance.setText("총 이동: " + d.getTotalDistanceText());
            tvCenter.setText(String.format("중심 %.3f, %.3f",
                    d.centerLat, d.centerLng));
        }
    }
}