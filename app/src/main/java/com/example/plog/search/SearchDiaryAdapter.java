/*
 * SearchDiaryAdapter
 *
 * [기능]
 * - 검색된 일기 데이터를 RecyclerView에 출력하는 Adapter
 * - 날짜, 감정, 제목, 내용, 위치 정보를 화면에 바인딩
 *
 * [구성]
 * - SearchDiary 데이터를 item_search_diary.xml UI와 연결
 * - RecyclerView ViewHolder 패턴 사용
 *
 * [추후 예정]
 * - Glide/Picasso를 이용한 이미지 URL 로딩 기능 추가 예정
 */
package com.example.plog.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;

import java.util.List;
import com.bumptech.glide.Glide;
import com.example.plog.util.Constants;
// 검색된 일기 데이터를 RecyclerView에 출력하는 Adapter
public class SearchDiaryAdapter extends RecyclerView.Adapter<SearchDiaryAdapter.ViewHolder> {

    // RecyclerView에 표시할 일기 목록
    private List<SearchDiary> diaryList;

    // Adapter 생성자
    public SearchDiaryAdapter(List<SearchDiary> diaryList) {
        this.diaryList = diaryList;
    }

    // Step 1. item_search_diary.xml 레이아웃 생성
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_search_diary,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    // Step 2. position 위치의 데이터를 View에 바인딩
    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        // 현재 위치의 일기 데이터 가져오기
        SearchDiary diary = diaryList.get(position);

        // 날짜 표시
        holder.tvDate.setText(
                diary.getDate()
        );

        // 감정 표시
        holder.tvEmotion.setText(
                diary.getEmotion()
        );

        // 제목 표시
        holder.tvTitle.setText(
                diary.getTitle()
        );

        // 내용 표시
        holder.tvContent.setText(
                diary.getContent()
        );

        // 위치 표시
        holder.tvLocation.setText(
                "📍 " + diary.getLocation()
        );

        // TODO: 이미지 URL 연동 예정
        String imageUrl = diary.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty() && !"null".equals(imageUrl)) {
            String fullImageUrl = imageUrl.startsWith("http")
                    ? imageUrl
                    : Constants.BASE_URL + imageUrl.replaceFirst("^/", "");

            Glide.with(holder.itemView.getContext())
                    .load(fullImageUrl)
                    .placeholder(R.drawable.bg_photo_placeholder)
                    .error(R.drawable.bg_photo_placeholder)
                    .centerCrop()
                    .into(holder.ivDiaryPhoto);
        } else {
            holder.ivDiaryPhoto.setImageResource(R.drawable.bg_photo_placeholder);
        }
    }

    // Step 3. RecyclerView 아이템 개수 반환
    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    // RecyclerView 아이템 UI를 저장하는 ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivDiaryPhoto;

        TextView tvDate;
        TextView tvEmotion;
        TextView tvTitle;
        TextView tvContent;
        TextView tvLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Step 4. XML 컴포넌트 연결
            ivDiaryPhoto =
                    itemView.findViewById(R.id.ivDiaryPhoto);

            tvDate =
                    itemView.findViewById(R.id.tvDate);

            tvEmotion =
                    itemView.findViewById(R.id.tvEmotion);

            tvTitle =
                    itemView.findViewById(R.id.tvTitle);

            tvContent =
                    itemView.findViewById(R.id.tvContent);

            tvLocation =
                    itemView.findViewById(R.id.tvLocation);
        }
    }
}