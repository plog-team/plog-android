// ui/photo/PhotoPickerFragment.java
package com.example.plog.ui.photo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PhotoPickerFragment extends Fragment {

    private PhotoViewModel viewModel;
    private PreviewAdapter previewAdapter;
    private ProgressBar    pbSaving;
    private Button         btnPick;

    private int           totalSelected = 0;
    private AtomicInteger savedCount    = new AtomicInteger(0);

    // ── 권한 요청 런처 ─────────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(), results -> {
                        // Android 14+: READ_MEDIA_IMAGES 거부 + READ_MEDIA_VISUAL_USER_SELECTED 허용
                        // = "선택한 사진만 허용" 상태 → GPS 추출 불가
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            boolean fullAccess    = isGranted(Manifest.permission.READ_MEDIA_IMAGES);
                            boolean partialAccess = isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
                            if (partialAccess && !fullAccess) {
                                showFullAccessRequiredDialog();
                                return;
                            }
                        }

                        Boolean locGranted = results.get(Manifest.permission.ACCESS_MEDIA_LOCATION);
                        if (Boolean.FALSE.equals(locGranted)) {
                            Toast.makeText(requireContext(),
                                    "위치 권한을 허용하면 GPS 정보를 저장할 수 있어요.",
                                    Toast.LENGTH_LONG).show();
                        }
                        openGallery();
                    });

    // ── 여러 장 선택 런처 ──────────────────────────────────────────────────
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickMultipleVisualMedia(), uris -> {
                        if (uris.isEmpty()) return;

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
        // Android 14+에서 이미 "부분 허용" 상태인지 먼저 확인
        // (이 경우 권한 다이얼로그가 다시 뜨지 않으므로 설정 화면으로 안내)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            boolean full    = isGranted(Manifest.permission.READ_MEDIA_IMAGES);
            boolean partial = isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            if (partial && !full) {
                showFullAccessRequiredDialog();
                return;
            }
        }

        List<String> toRequest = new ArrayList<>();

        // READ_MEDIA_IMAGES (API 33+) — MediaStore URI 직접 접근으로 GPS 추출
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !isGranted(Manifest.permission.READ_MEDIA_IMAGES)) {
            toRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            // API 34+: 부분 허용 흐름 지원
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                toRequest.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            }
        }

        // ACCESS_MEDIA_LOCATION (API 29+) — MediaStore FD에서 GPS 보존
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !isGranted(Manifest.permission.ACCESS_MEDIA_LOCATION)) {
            toRequest.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
        }

        Log.d("Permission", "요청할 권한: " + toRequest);

        if (toRequest.isEmpty()) {
            openGallery();
        } else {
            requestPermissionsLauncher.launch(toRequest.toArray(new String[0]));
        }
    }

    /**
     * Android 14+에서 "선택한 사진만 허용" 상태일 때 표시.
     * GPS 추출에는 MediaStore 전체 접근이 필요하므로 설정으로 안내.
     */
    private void showFullAccessRequiredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("사진 접근 권한 변경 필요")
                .setMessage("GPS 정보를 저장하려면 사진 접근 권한을 '모든 사진 허용'으로 변경해야 합니다.\n\n"
                        + "설정 → 앱 → Plog → 권한 → 사진 및 동영상 → 모든 사진 허용")
                .setPositiveButton("설정으로 이동", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("GPS 없이 계속", (d, w) -> openGallery())
                .show();
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ── 갤러리 열기 ────────────────────────────────────────────────────────
    private void openGallery() {
        pickMediaLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }
}
