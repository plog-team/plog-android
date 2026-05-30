package com.example.plog.ui.exchange;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.network.dto.ExchangeMatchResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PendingMatchAdapter extends RecyclerView.Adapter<PendingMatchAdapter.ViewHolder> {

    private List<ExchangeMatchResponse> list;
    private final OnAcceptListener onAccept;
    private final OnRejectListener onReject;

    public interface OnAcceptListener { void onAccept(Long matchId); }
    public interface OnRejectListener { void onReject(Long matchId); }

    public PendingMatchAdapter(List<ExchangeMatchResponse> list, OnAcceptListener onAccept, OnRejectListener onReject) {
        this.list = list;
        this.onAccept = onAccept;
        this.onReject = onReject;
    }

    public void updateList(List<ExchangeMatchResponse> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public String getNickname(Long matchId) {
        for (ExchangeMatchResponse match : list) {
            if (match.getId().equals(matchId)) {
                return match.getRequesterNickname() != null ? match.getRequesterNickname() : "사용자";
            }
        }
        return "사용자";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExchangeMatchResponse match = list.get(position);
        String name = match.getRequesterNickname() != null ? match.getRequesterNickname() : "사용자";
        holder.tvMatchId.setText(name + "님의 매칭 신청");
        holder.btnAccept.setOnClickListener(v -> onAccept.onAccept(match.getId()));
        holder.btnReject.setOnClickListener(v -> onReject.onReject(match.getId()));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMatchId;
        MaterialButton btnAccept, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvMatchId = itemView.findViewById(R.id.tvMatchId);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}