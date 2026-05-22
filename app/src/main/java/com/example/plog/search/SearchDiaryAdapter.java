package com.example.plog.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.plog.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchDiaryAdapter extends RecyclerView.Adapter<SearchDiaryAdapter.ViewHolder> {

    private List<SearchDiary> diaryList;

    public SearchDiaryAdapter(List<SearchDiary> diaryList) {
        this.diaryList = diaryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_diary, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchDiary diary = diaryList.get(position);

        holder.tvDate.setText(diary.getDate());
        holder.tvEmotion.setText(diary.getEmotion());
        holder.tvTitle.setText(diary.getTitle());
        holder.tvContent.setText(diary.getContent());
        holder.tvLocation.setText("📍 " + diary.getLocation());

        // imageUrl은 나중에 Glide 연결
        // Glide.with(holder.itemView.getContext())
        //      .load(diary.getImageUrl())
        //      .into(holder.ivDiaryPhoto);
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivDiaryPhoto;
        TextView tvDate, tvEmotion, tvTitle, tvContent, tvLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivDiaryPhoto = itemView.findViewById(R.id.ivDiaryPhoto);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvEmotion = itemView.findViewById(R.id.tvEmotion);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}