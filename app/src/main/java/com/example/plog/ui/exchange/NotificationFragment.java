package com.example.plog.ui.exchange;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.NotificationApi;
import com.example.plog.network.dto.NotificationResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationFragment extends DialogFragment {

    private RecyclerView rvNotifications;
    private TextView tvNoNotification;
    private NotificationAdapter adapter;

    public NotificationFragment() {}

    private long getMyUserId() {
        return requireActivity()
                .getSharedPreferences("plog_prefs", Context.MODE_PRIVATE)
                .getLong("userId", -1L);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvNoNotification = view.findViewById(R.id.tvNoNotification);

        adapter = new NotificationAdapter(new ArrayList<>(), this::markAsRead);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        loadNotifications();
        return view;
    }

    private void loadNotifications() {
        NotificationApi api = RetrofitClient.getClient().create(NotificationApi.class);
        api.getNotifications(getMyUserId()).enqueue(new Callback<List<NotificationResponse>>() {
            @Override
            public void onResponse(Call<List<NotificationResponse>> call, Response<List<NotificationResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationResponse> list = response.body();
                    adapter.updateList(list);
                    tvNoNotification.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<List<NotificationResponse>> call, Throwable t) {
                android.util.Log.e("Notification", "로드 실패: " + t.getMessage());
            }
        });
    }

    private void markAsRead(Long notificationId) {
        NotificationApi api = RetrofitClient.getClient().create(NotificationApi.class);
        api.markAsRead(notificationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                loadNotifications();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.util.Log.e("Notification", "읽음 처리 실패: " + t.getMessage());
            }
        });
    }
}