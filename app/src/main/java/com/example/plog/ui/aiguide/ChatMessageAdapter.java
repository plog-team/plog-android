package com.example.plog.ui.aiguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.model.ChatMessageDto;
import com.example.plog.ui.util.TypingDotsAnimator;
import com.example.plog.ui.util.TypingEffect;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;
    /** AI 응답 대기 중 표시되는 placeholder 마커. */
    public static final String TYPING_PLACEHOLDER = "···";

    private final List<ChatMessageDto> items = new ArrayList<>();
    /** 새로 추가된 ASSISTANT 메시지는 typing effect로 표시 (한 번만). */
    private int typingTargetIdx = -1;

    public void addMessage(ChatMessageDto m) {
        items.add(m);
        // ASSISTANT 메시지 + placeholder 아닐 때 typing 효과 대상
        if ("ASSISTANT".equals(m.role) && !TYPING_PLACEHOLDER.equals(m.content)) {
            typingTargetIdx = items.size() - 1;
        }
        notifyItemInserted(items.size() - 1);
    }

    /** AI 응답 대기 placeholder 추가. */
    public int addTypingPlaceholder() {
        ChatMessageDto m = new ChatMessageDto("ASSISTANT", TYPING_PLACEHOLDER);
        items.add(m);
        notifyItemInserted(items.size() - 1);
        return items.size() - 1;
    }

    /** 마지막 ASSISTANT placeholder를 실제 응답으로 교체. */
    public void replaceLastAssistant(String realText) {
        if (items.isEmpty()) return;
        int last = items.size() - 1;
        if (!"ASSISTANT".equals(items.get(last).role)) return;
        items.set(last, new ChatMessageDto("ASSISTANT", realText));
        typingTargetIdx = last;
        notifyItemChanged(last);
    }

    public int size() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return "USER".equals(items.get(position).role) ? TYPE_USER : TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_assistant;
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MsgVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MsgVH vh = (MsgVH) holder;
        String content = items.get(position).content;
        // placeholder면 dots 애니메이션으로 AI 응답 대기 중 표시
        if (TYPING_PLACEHOLDER.equals(content)) {
            TypingEffect.cancel(vh.tvContent);
            TypingDotsAnimator.start(vh.tvContent);
            return;
        }
        // 실제 메시지면 dots 애니메이션 중단 + (새 ASSISTANT 메시지인 경우) typing 효과
        TypingDotsAnimator.stop(vh.tvContent);
        if (position == typingTargetIdx) {
            TypingEffect.apply(vh.tvContent, content, 22L);
            typingTargetIdx = -1; // 한 번만 적용
        } else {
            vh.tvContent.setText(content);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MsgVH extends RecyclerView.ViewHolder {
        final TextView tvContent;
        MsgVH(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
        }
    }
}
