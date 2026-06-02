package com.example.plog.ui.recommend.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.ui.recommend.model.PlaceItem;
import java.util.List;

public class FeaturedAdapter
        extends RecyclerView.Adapter<FeaturedAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PlaceItem item);
    }

    private List<PlaceItem> items;
    private final OnItemClickListener listener;

    public FeaturedAdapter(List<PlaceItem> items,
                           OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    public void updateItems(List<PlaceItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_featured,
                        parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle, tvAddress, tvDistance, tvCategory;

        ViewHolder(View v) {
            super(v);
            ivThumbnail = v.findViewById(R.id.ivThumbnail);
            tvTitle     = v.findViewById(R.id.tvTitle);
            tvAddress   = v.findViewById(R.id.tvAddress);
            tvDistance  = v.findViewById(R.id.tvDistance);
            tvCategory  = v.findViewById(R.id.tvCategory);
        }

        void bind(PlaceItem item, OnItemClickListener l) {
            tvTitle.setText(item.getTitle());
            tvAddress.setText(item.getAddress());
            tvDistance.setText(formatDist(item.getDistance()));
            tvCategory.setText(item.getCategory());

            if (item.getImageUrl() != null
                    && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .centerCrop()
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setImageResource(
                        R.drawable.gradient_diary_banner);
            }
            itemView.setOnClickListener(v -> l.onItemClick(item));
        }

        private String formatDist(String dist) {
            try {
                double d = Double.parseDouble(dist);
                if (d < 1000) return (int) d + "m";
                return String.format("%.1fkm", d / 1000);
            } catch (Exception e) { return dist + "m"; }
        }
    }
}