package com.example.plog.ui.photo;

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

public class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.PreviewVH> {

    private final List<Uri> uriList = new ArrayList<>();

    public void setUris(List<Uri> uris) {
        uriList.clear();
        uriList.addAll(uris);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PreviewVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview, parent, false);
        return new PreviewVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewVH holder, int position) {
        holder.bind(uriList.get(position));
    }

    @Override
    public int getItemCount() { return uriList.size(); }

    static class PreviewVH extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        PreviewVH(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.iv_preview_item);
        }
        void bind(Uri uri) {
            Glide.with(ivPhoto.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(ivPhoto);
        }
    }
}