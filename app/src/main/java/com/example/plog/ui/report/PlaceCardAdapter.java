package com.example.plog.ui.report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.model.PlaceEntry;

import java.util.List;

public class PlaceCardAdapter extends RecyclerView.Adapter<PlaceCardAdapter.ViewHolder> {

    private final List<PlaceEntry> places;

    public PlaceCardAdapter(List<PlaceEntry> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_place_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceEntry entry = places.get(position);
        holder.tvPlaceName.setText(entry.placeName);
        holder.tvPlaceCount.setText(entry.count + "회 방문");
        holder.tvMainEmotion.setText(entry.mainEmotion != null ? entry.mainEmotion : "-");
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName;
        TextView tvPlaceCount;
        TextView tvMainEmotion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvPlaceCount = itemView.findViewById(R.id.tvPlaceCount);
            tvMainEmotion = itemView.findViewById(R.id.tvMainEmotion);
        }
    }
}
