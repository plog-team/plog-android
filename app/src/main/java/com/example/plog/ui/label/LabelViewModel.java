// ui/label/LabelViewModel.java
package com.example.plog.ui.label;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.plog.data.repository.LabelRepository;
import com.example.plog.model.LabelResult;
import com.example.plog.util.ImageLabelerHelper;
import com.example.plog.util.ImageLabelerHelper.LabelingCallback;

import java.util.List;

public class LabelViewModel extends AndroidViewModel {

    private final MutableLiveData<List<LabelResult>> labelResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean>           isLoading    = new MutableLiveData<>(false);
    private final MutableLiveData<String>            errorMsg     = new MutableLiveData<>();

    private final ImageLabelerHelper labelerHelper;
    private final LabelRepository    repository;     // ← LabelRepository 사용

    public LabelViewModel(@NonNull Application application) {
        super(application);
        labelerHelper = new ImageLabelerHelper(ImageLabelerHelper.THRESHOLD_DEFAULT);
        repository    = new LabelRepository(application);
    }

    public LiveData<List<LabelResult>> getLabelResults() { return labelResults; }
    public LiveData<Boolean>           getIsLoading()    { return isLoading;    }
    public LiveData<String>            getErrorMsg()     { return errorMsg;     }

    /**
     * ML Kit 분석 → RecyclerView 업데이트 + LabelRepository로 DB 저장
     *
     * @param uri     분석할 이미지 URI
     * @param photoId PhotoRepository.savePhoto()가 반환한 photo.id
     */
    public void analyzeAndSave(@NonNull Uri uri, int photoId) {
        isLoading.setValue(true);
        errorMsg.setValue(null);

        labelerHelper.analyze(uri, getApplication(), new LabelingCallback() {

            @Override
            public void onSuccess(@NonNull List<LabelResult> labels) {
                isLoading.setValue(false);
                labelResults.setValue(labels);           // RecyclerView 업데이트
                repository.saveLabels(photoId, labels);  // DB 저장 (내부에서 IO 스레드)
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                isLoading.setValue(false);
                errorMsg.setValue("분석 실패: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        labelerHelper.close();
    }
}