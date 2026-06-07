package com.example.plog.ui.exchange;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.network.dto.NotificationResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationResponse> list;
    private final OnReadListener onRead;

    public interface OnReadListener { void onRead(Long notificationId); }

    public NotificationAdapter(List<NotificationResponse> list, OnReadListener onRead) {
        this.list = list;
        this.onRead = onRead;
    }

    public void updateList(List<NotificationResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationResponse notification = list.get(position);
        holder.tvMessage.setText(notification.getMessage());
        holder.tvCreatedAt.setText(notification.getCreatedAt());
        holder.itemView.setAlpha(notification.isRead() ? 0.5f : 1.0f);
        holder.btnRead.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        holder.btnRead.setOnClickListener(v -> onRead.onRead(notification.getId()));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvCreatedAt;
        MaterialButton btnRead;

        ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            btnRead = itemView.findViewById(R.id.btnRead);
        }
    }
}