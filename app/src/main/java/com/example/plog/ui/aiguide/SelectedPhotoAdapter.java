package com.example.plog.ui.aiguide;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.plog.R;

import java.util.ArrayList;
import java.util.List;

public class SelectedPhotoAdapter extends RecyclerView.Adapter<SelectedPhotoAdapter.PhotoVH> {

    private final List<Uri> items = new ArrayList<>();

    public void setItems(List<Uri> uris) {
        items.clear();
        if (uris != null) items.addAll(uris);
        notifyDataSetChanged();
    }

    public int size() {
        return items.size();
    }

    @NonNull
    @Override
    public PhotoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_photo, parent, false);
        return new PhotoVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoVH holder, int position) {
        Uri uri = items.get(position);
        Glide.with(holder.image.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PhotoVH extends RecyclerView.ViewHolder {
        final ImageView image;

        PhotoVH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.photoThumb);
        }
    }
}
