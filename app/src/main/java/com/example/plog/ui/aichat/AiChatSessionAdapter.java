package com.example.plog.ui.aichat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.plog.R;
import com.example.plog.network.aichat.AiChatSessionListResponse;
import java.util.ArrayList;
import java.util.List;

public class AiChatSessionAdapter extends RecyclerView.Adapter<AiChatSessionAdapter.ViewHolder> {

    private List<AiChatSessionListResponse.Session> sessions = new ArrayList<>();
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(long sessionId);
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<AiChatSessionListResponse.Session> sessions) {
        this.sessions = sessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiChatSessionListResponse.Session session = sessions.get(position);

        holder.tvTitle.setText(session.title != null ? session.title : "새 대화");
        holder.tvDate.setText(session.createdAt != null ? session.createdAt.substring(0, 10) : "");
        holder.tvEmotion.setText(emotionToEmoji(session.emotion));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(session.sessionId);
        });
    }

    @Override
    public int getItemCount() { return sessions.size(); }

    private String emotionToEmoji(String emotion) {
        if (emotion == null) return "💬";
        switch (emotion) {
            case "기쁨": return "😊";
            case "슬픔": return "😢";
            case "불안": return "😰";
            case "분노": return "😠";
            case "평온": return "😌";
            case "혼란": return "😕";
            case "설렘": return "🥰";
            default: return "💬";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvEmotion;
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvEmotion = itemView.findViewById(R.id.tv_emotion);
        }
    }
}