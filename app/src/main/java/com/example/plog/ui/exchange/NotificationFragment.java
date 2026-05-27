package com.example.plog.ui.exchange;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.plog.R;
import com.google.android.material.button.MaterialButton;

public class NotificationFragment extends DialogFragment {

    public NotificationFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_notification,
                container,
                false
        );
    }

    @Override
    public void onStart() {

        super.onStart();

        if (getDialog() != null
                && getDialog().getWindow() != null) {

            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );

            getDialog().getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );
        }
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle =
                view.findViewById(
                        R.id.tvNotificationTitle
                );

        TextView tvMessage =
                view.findViewById(
                        R.id.tvNotificationMessage
                );

        MaterialButton btnClose =
                view.findViewById(
                        R.id.btnCloseNotification
                );

        String title = null;
        String message = null;

        if (getArguments() != null) {

            title =
                    getArguments().getString(
                            "title"
                    );

            message =
                    getArguments().getString(
                            "message"
                    );
        }

        tvTitle.setText(title);
        tvMessage.setText(message);

        btnClose.setOnClickListener(v -> dismiss());
    }
}