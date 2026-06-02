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

public class RecommendAdapter
        extends RecyclerView.Adapter<RecommendAdapter.ViewHolder> {

    private List<PlaceItem> items;
    private final FeaturedAdapter.OnItemClickListener listener;

    public RecommendAdapter(List<PlaceItem> items,
                            FeaturedAdapter.OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        h.bind(items.get(pos), listener);
    }

    @Override public int getItemCount() { return items.size(); }

    public void updateItems(List<PlaceItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

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

        void bind(PlaceItem item, FeaturedAdapter.OnItemClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvAddress.setText(item.getAddress());
            tvCategory.setText(item.getCategory());
            tvDistance.setText(formatDist(item.getDistance()));
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView).load(item.getImageUrl()).centerCrop().into(ivThumbnail);
            } else {
                ivThumbnail.setImageResource(R.drawable.gradient_ai_banner);
            }
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        private String formatDist(String dist) {
            try {
                double d = Double.parseDouble(dist);
                return d < 1000 ? (int)d + "m" : String.format("%.1fkm", d/1000);
            } catch (Exception e) { return dist + "m"; }
        }
    }
}