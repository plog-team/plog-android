package com.example.plog.ui.aiguide;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.plog.R;
import com.example.plog.databinding.FragmentAiGuideEntryBinding;
import com.example.plog.model.ApiResponse;
import com.example.plog.model.CreateSessionRequest;
import com.example.plog.model.CreateSessionResponse;
import com.example.plog.model.PhotoUploadBatchResponse;
import com.example.plog.network.ApiClient;
import com.example.plog.network.ApiService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiGuideEntryFragment extends Fragment {

    private static final String TAG = "AiGuideEntry";
    private static final int MAX_PHOTOS = 10;

    private FragmentAiGuideEntryBinding binding;
    private SelectedPhotoAdapter adapter;
    private final List<Uri> selectedUris = new ArrayList<>();
    private final ApiService api = ApiClient.getApiService();

    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris == null || uris.isEmpty()) return;
                if (uris.size() > MAX_PHOTOS) {
                    Toast.makeText(getContext(), "최대 " + MAX_PHOTOS + "장까지 선택 가능합니다", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedUris.clear();
                selectedUris.addAll(uris);
                adapter.setItems(selectedUris);
                binding.tvPickedSummary.setText("선택된 사진 " + selectedUris.size() + "장");
                binding.btnStart.setEnabled(true);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiGuideEntryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new SelectedPhotoAdapter();
        binding.rvSelected.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvSelected.setAdapter(adapter);

        binding.btnPick.setOnClickListener(v -> picker.launch(new String[]{"image/*"}));
        binding.btnStart.setOnClickListener(v -> startAiGuide());
    }

    private void startAiGuide() {
        if (selectedUris.isEmpty()) {
            Toast.makeText(getContext(), "먼저 사진을 선택해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress(true, "사진 업로드 중 (0/" + selectedUris.size() + ")");
        hideResults();
        binding.btnStart.setEnabled(false);
        binding.btnPick.setEnabled(false);

        List<Long> photoIds = new ArrayList<>(selectedUris.size());
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        final int total = selectedUris.size();

        for (Uri uri : selectedUris) {
            uploadOne(uri, new UploadCallback() {
                @Override
                public void onSuccess(Long photoId) {
                    synchronized (photoIds) {
                        photoIds.add(photoId);
                    }
                    int done = completed.incrementAndGet();
                    if (binding != null) {
                        binding.tvProgress.setText("사진 업로드 중 (" + done + "/" + total + ")");
                    }
                    if (done + failed.get() == total) {
                        if (photoIds.isEmpty()) {
                            onAllUploadsFailed("모든 업로드 실패");
                        } else {
                            createSession(new ArrayList<>(photoIds));
                        }
                    }
                }

                @Override
                public void onFailure(String msg) {
                    failed.incrementAndGet();
                    Log.w(TAG, "upload failed uri=" + uri + " : " + msg);
                    if (completed.get() + failed.get() == total) {
                        if (photoIds.isEmpty()) {
                            onAllUploadsFailed("업로드 실패: " + msg);
                        } else {
                            createSession(new ArrayList<>(photoIds));
                        }
                    }
                }
            });
        }
    }

    private void uploadOne(Uri uri, UploadCallback cb) {
        ContentResolver cr = requireContext().getContentResolver();
        byte[] bytes;
        try (InputStream is = cr.openInputStream(uri)) {
            if (is == null) {
                cb.onFailure("InputStream null");
                return;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            cb.onFailure("IO: " + e.getMessage());
            return;
        }
        String mime = cr.getType(uri);
        if (mime == null) mime = "image/jpeg";
        String filename = "photo_" + System.currentTimeMillis() + extFromMime(mime);
        RequestBody body = RequestBody.create(bytes, MediaType.parse(mime));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, body);

        api.uploadPhoto(part).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PhotoUploadBatchResponse>> call, @NonNull Response<ApiResponse<PhotoUploadBatchResponse>> resp) {
                if (resp.isSuccessful()
                        && resp.body() != null
                        && resp.body().data != null
                        && resp.body().data.photos != null
                        && !resp.body().data.photos.isEmpty()) {
                    cb.onSuccess(resp.body().data.photos.get(0).photoId);
                } else {
                    cb.onFailure("HTTP " + resp.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PhotoUploadBatchResponse>> call, @NonNull Throwable t) {
                cb.onFailure(t.getMessage());
            }
        });
    }

    private void createSession(List<Long> photoIds) {
        if (binding != null) binding.tvProgress.setText("AI 분석 중 (" + photoIds.size() + "장)");
        String mode = (binding != null && binding.rgMode.getCheckedRadioButtonId() == R.id.rbModeConversation)
                ? "CONVERSATION" : "BATCH";
        String persona = "DEFAULT";
        if (binding != null) {
            int pid = binding.rgPersona.getCheckedRadioButtonId();
            if (pid == R.id.rbPersonaDefault) persona = "DEFAULT";
            else if (pid == R.id.rbPersonaFriendly) persona = "FRIENDLY";
            else if (pid == R.id.rbPersonaEmotional) persona = "EMOTIONAL";
            else if (pid == R.id.rbPersonaFormal) persona = "FORMAL";
            else if (pid == R.id.rbPersonaWitty) persona = "WITTY";
        }
        api.createAiSession(new CreateSessionRequest(photoIds, mode, persona)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<CreateSessionResponse>> call, @NonNull Response<ApiResponse<CreateSessionResponse>> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().data != null) {
                    renderResult(resp.body().data);
                } else {
                    onAllUploadsFailed("세션 생성 실패: HTTP " + resp.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<CreateSessionResponse>> call, @NonNull Throwable t) {
                onAllUploadsFailed("세션 생성 실패: " + t.getMessage());
            }
        });
    }

    private void renderResult(CreateSessionResponse r) {
        if (binding == null || getView() == null) return;
        showProgress(false, null);
        binding.btnStart.setEnabled(true);
        binding.btnPick.setEnabled(true);
        if ("CONVERSATION".equals(r.mode)) {
            Bundle args = AiGuideChatFragment.argsOf(r.sessionId, r.firstAssistantMessage);
            Navigation.findNavController(getView()).navigate(R.id.action_entry_to_chat, args);
        } else {
            // 첫 사진 vision 결과를 Session 화면으로 전달
            String scene = null, mood = null, emotion = null, timeOfDay = null, objectsCsv = null;
            if (r.photos != null && !r.photos.isEmpty() && r.photos.get(0).vision != null) {
                com.example.plog.model.VisionResult v = r.photos.get(0).vision;
                scene = v.scene;
                mood = v.mood;
                emotion = v.suggestedEmotion;
                timeOfDay = v.timeOfDay;
                if (v.objects != null && !v.objects.isEmpty()) {
                    objectsCsv = String.join(",", v.objects);
                }
            }
            Bundle args = AiGuideSessionFragment.argsOf(r.sessionId, scene, mood, emotion, timeOfDay, objectsCsv);
            Navigation.findNavController(getView()).navigate(R.id.action_entry_to_session, args);
        }
    }

    private void onAllUploadsFailed(String reason) {
        showProgress(false, null);
        if (binding != null) {
            binding.btnStart.setEnabled(true);
            binding.btnPick.setEnabled(true);
        }
        Toast.makeText(getContext(), reason, Toast.LENGTH_LONG).show();
    }

    private void showProgress(boolean show, String text) {
        if (binding == null) return;
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.tvProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (text != null) binding.tvProgress.setText(text);
    }

    private void hideResults() {
        if (binding == null) return;
        binding.tvResultTitle.setVisibility(View.GONE);
        binding.tvVisionResult.setVisibility(View.GONE);
        binding.tvQuestionsResult.setVisibility(View.GONE);
    }

    private static String extFromMime(String mime) {
        if (mime == null) return ".jpg";
        if (mime.contains("png")) return ".png";
        if (mime.contains("webp")) return ".webp";
        if (mime.contains("gif")) return ".gif";
        return ".jpg";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface UploadCallback {
        void onSuccess(Long photoId);
        void onFailure(String msg);
    }
}
