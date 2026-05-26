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

// RecyclerView에 일기 데이터를 표시하기 위한 Adapter 클래스
public class SearchDiaryAdapter extends RecyclerView.Adapter<SearchDiaryAdapter.ViewHolder> {

    // RecyclerView에 표시할 일기 목록 저장
    private List<SearchDiary> diaryList;

    // Adapter 생성자 : 전달받은 일기 리스트 저장
    public SearchDiaryAdapter(List<SearchDiary> diaryList) {
        this.diaryList = diaryList;
    }

    // RecyclerView 아이템(XML) 생성
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // item_search_diary.xml을 View로 변환(inflate)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_diary, parent, false);

        return new ViewHolder(view);
    }

    // 각 위치(position)의 데이터를 View에 바인딩
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        // 현재 위치의 일기 데이터 가져오기
        SearchDiary diary = diaryList.get(position);

        // TextView에 데이터 설정
        holder.tvDate.setText(diary.getDate());         // 날짜
        holder.tvEmotion.setText(diary.getEmotion());   // 감정
        holder.tvTitle.setText(diary.getTitle());       // 제목
        holder.tvContent.setText(diary.getContent());   // 내용
        holder.tvLocation.setText("📍 " + diary.getLocation()); // 위치

        // 이미지 URL 연결 (추후 Glide/Picasso 사용 예정)
        /*
        Glide.with(holder.itemView.getContext())
             .load(diary.getImageUrl())
             .into(holder.ivDiaryPhoto);
        */
    }

    // RecyclerView 아이템 개수 반환
    @Override
    public int getItemCount() {
        return diaryList.size();
    }


    // 각 아이템(View)의 UI 요소 저장하는 ViewHolder 클래스
    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivDiaryPhoto;
        TextView tvDate, tvEmotion, tvTitle, tvContent, tvLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // XML 컴포넌트 연결
            ivDiaryPhoto = itemView.findViewById(R.id.ivDiaryPhoto); // 사진
            tvDate = itemView.findViewById(R.id.tvDate);             // 날짜
            tvEmotion = itemView.findViewById(R.id.tvEmotion);       // 감정
            tvTitle = itemView.findViewById(R.id.tvTitle);           // 제목
            tvContent = itemView.findViewById(R.id.tvContent);       // 내용
            tvLocation = itemView.findViewById(R.id.tvLocation);     // 위치
        }
    }
}