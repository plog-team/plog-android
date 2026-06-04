// ui/photo/PhotoViewModel.java
package com.example.plog.ui.photo;
import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.plog.data.repository.LabelRepository;
import com.example.plog.data.repository.PhotoRepository;
import com.example.plog.util.Constants;
import com.example.plog.util.ImageLabelerHelper;
import com.example.plog.util.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.plog.autofill.PhotoSaveResult;
public class PhotoViewModel extends AndroidViewModel {

    private final MutableLiveData<Long>    savedPhotoId = new MutableLiveData<>();
    //자동입력용 객체
    private final MutableLiveData<PhotoSaveResult> savedPhotoResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading    = new MutableLiveData<>(false);
    private final MutableLiveData<String>  errorMsg     = new MutableLiveData<>();

    private final PhotoRepository    photoRepository;
    private final LabelRepository    labelRepository;
    private final ImageLabelerHelper labelerHelper;
    private final SessionManager     sessionManager;
    private final ExecutorService    executor = Executors.newSingleThreadExecutor();

    public PhotoViewModel(@NonNull Application application) {
        super(application);
        photoRepository = new PhotoRepository(application);
        labelRepository = new LabelRepository(application);
        labelerHelper   = new ImageLabelerHelper(ImageLabelerHelper.THRESHOLD_DEFAULT);
        sessionManager  = new SessionManager(application);
    }

    public LiveData<Long>    getSavedPhotoId() { return savedPhotoId; }
    public LiveData<Boolean> getIsLoading()    { return isLoading;    }
    public LiveData<String>  getErrorMsg()     { return errorMsg;     }

    /** 사진 저장 결과를 원본 URI와 photoId로 함께 반환 */
    public LiveData<PhotoSaveResult> getSavedPhotoResult() {
        return savedPhotoResult;
    }

    /** 갤러리 URI로 서버 photoId 조회 */
    public Long getServerPhotoIdByImageUrl(String imageUrl) {
        return photoRepository.getServerPhotoIdByImageUrl(imageUrl);
    }

    /** 일기 수정 시 교체된 갤러리 URI를 DB에서 소프트 삭제 */
    public void removePhotoByUrl(@NonNull String imageUrl) {
        executor.execute(() -> photoRepository.softDeleteByImageUrl(imageUrl));
    }

    public void processPhoto(@NonNull Uri uri) {
        int raw = sessionManager.getUserId();
        int userId = (raw == -1) ? (int) Constants.DEV_USER_ID : raw;

        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                // ── 1. 사진 + EXIF(GPS) 저장 ──────────────────────────────
                long photoId = photoRepository.savePhoto(uri, userId, getApplication());

                if (photoId == -1) {
                    errorMsg.postValue("사진 저장 실패");
                    return;
                }

                // ── 2. ML Kit 라벨링 + 저장 ───────────────────────────────
                // ML Kit은 메인 스레드 콜백이므로 별도 처리
                labelerHelper.analyze(uri, getApplication(),
                        new ImageLabelerHelper.LabelingCallback() {

                            @Override
                            public void onSuccess(
                                    @NonNull java.util.List<com.example.plog.model.LabelResult> labels) {
                                labelRepository.saveLabels((int) photoId, labels);
                                android.util.Log.d("PhotoViewModel",
                                        "라벨 저장 완료 — " + labels.size() + "개");
                            }

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                android.util.Log.e("PhotoViewModel",
                                        "라벨링 실패: " + e.getMessage());
                            }
                        });

                // ── 3. 저장 완료 알림 ──────────────────────────────────────
                savedPhotoId.postValue(photoId);
                savedPhotoResult.postValue(new PhotoSaveResult(uri.toString(), photoId));
                android.util.Log.d("PhotoViewModel",
                        "전체 저장 완료 — photoId: " + photoId);

            } catch (Exception e) {
                errorMsg.postValue("저장 실패: " + e.getMessage());
                android.util.Log.e("PhotoViewModel", "저장 실패", e);
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        labelerHelper.close();
        executor.shutdown();
    }
}