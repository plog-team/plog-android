package com.example.plog.ui.my;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.api.model.BookmarkItem;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(BookmarkItem item, int position);
    }

    private final List<BookmarkItem> items;
    private final OnRemoveClickListener removeListener;

    public BookmarkAdapter(List<BookmarkItem> items, OnRemoveClickListener removeListener) {
        this.items = items;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bookmark, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        BookmarkItem item = items.get(pos);
        h.tvTitle.setText(item.title);
        h.tvAddress.setText(item.address != null ? item.address : "");
        h.tvCategory.setText(item.category != null ? item.category : "");
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(h.ivThumb.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.bg_user_circle)
                    .error(R.drawable.bg_user_circle)
                    .centerCrop()
                    .into(h.ivThumb);
        } else {
            h.ivThumb.setImageResource(R.drawable.bg_user_circle);
        }
        h.ivRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(item, h.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeAt(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb, ivRemove;
        TextView tvTitle, tvAddress, tvCategory;

        ViewHolder(View v) {
            super(v);
            ivThumb    = v.findViewById(R.id.ivBookmarkThumb);
            tvTitle    = v.findViewById(R.id.tvBookmarkTitle);
            tvAddress  = v.findViewById(R.id.tvBookmarkAddress);
            tvCategory = v.findViewById(R.id.tvBookmarkCategory);
            ivRemove   = v.findViewById(R.id.ivBookmarkRemove);
        }
    }
}
