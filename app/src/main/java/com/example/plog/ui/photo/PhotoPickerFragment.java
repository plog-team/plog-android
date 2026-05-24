// ui/photo/PhotoPickerFragment.java
package com.example.plog.ui.photo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.plog.R;
import com.example.plog.util.SessionManager;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class PhotoPickerFragment extends Fragment {

    private PhotoViewModel viewModel;
    private PreviewAdapter previewAdapter;
    private ProgressBar    pbSaving;
    private Button         btnPick;

    private int           totalSelected = 0;
    private AtomicInteger savedCount    = new AtomicInteger(0);

    // ── 위치 권한 요청 런처 ────────────────────────────────────────────────
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            openGallery();
                        } else {
                            Toast.makeText(requireContext(),
                                    "위치 권한을 허용하면 GPS 정보를 저장할 수 있어요.",
                                    Toast.LENGTH_LONG).show();
                            // 권한 없어도 갤러리는 열기
                            openGallery();
                        }
                    });

    // ── 여러 장 선택 런처 ──────────────────────────────────────────────────
    // 기존 PickMultipleVisualMedia 런처 교체
    private final ActivityResultLauncher<Intent> pickMediaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() != Activity.RESULT_OK) return;
                        if (result.getData() == null) return;

                        List<Uri> uris = new ArrayList<>();

                        // 여러 장 선택
                        if (result.getData().getClipData() != null) {
                            ClipData clipData = result.getData().getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                uris.add(clipData.getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) {
                            // 한 장 선택
                            uris.add(result.getData().getData());
                        }

                        if (uris.isEmpty()) return;

                        for (Uri uri : uris) {
                            requireContext().getContentResolver()
                                    .takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    );
                        }

                        previewAdapter.setUris(uris);
                        totalSelected = uris.size();
                        savedCount.set(0);
                        pbSaving.setVisibility(View.VISIBLE);
                        btnPick.setEnabled(false);

                        for (Uri uri : uris) {
                            viewModel.processPhoto(uri);
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 임시 userId 설정
        SessionManager sessionManager = new SessionManager(requireContext());
        if (!sessionManager.isLoggedIn()) {
            sessionManager.saveUserId(1);
        }

        btnPick  = view.findViewById(R.id.btn_pick);
        pbSaving = view.findViewById(R.id.pb_saving);

        RecyclerView rvPreview = view.findViewById(R.id.rv_preview);
        previewAdapter = new PreviewAdapter();
        rvPreview.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvPreview.setAdapter(previewAdapter);

        viewModel = new ViewModelProvider(this).get(PhotoViewModel.class);

        // ── 버튼 클릭 → 권한 확인 후 갤러리 열기 ─────────────────────────
        btnPick.setOnClickListener(v -> checkPermissionAndOpenGallery());

        // ── 저장 완료 → 리포트로 이동 ─────────────────────────────────────
        viewModel.getSavedPhotoId().observe(getViewLifecycleOwner(), photoId -> {
            if (photoId == null || photoId == -1) return;
            if (totalSelected == 0) return;

            int done = savedCount.incrementAndGet();

            if (done >= totalSelected) {
                pbSaving.setVisibility(View.GONE);
                btnPick.setEnabled(true);
                Toast.makeText(requireContext(),
                        totalSelected + "장 저장 완료!", Toast.LENGTH_SHORT).show();

                totalSelected = 0;
                savedCount.set(0);

                Navigation.findNavController(view)
                        .navigate(R.id.action_photoPickerFragment_to_analysisFragment);
            }
        });

        viewModel.getErrorMsg().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                pbSaving.setVisibility(View.GONE);
                btnPick.setEnabled(true);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── 권한 확인 후 갤러리 열기 ───────────────────────────────────────────
    private void checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean granted = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_MEDIA_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;

            Log.d("Permission", "ACCESS_MEDIA_LOCATION 허용 여부: " + granted);

            if (granted) {
                openGallery();
            } else {
                requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_MEDIA_LOCATION);
            }
        } else {
            openGallery();
        }
    }

    // ── 갤러리 열기 ────────────────────────────────────────────────────────
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        pickMediaLauncher.launch(intent);
    }
}