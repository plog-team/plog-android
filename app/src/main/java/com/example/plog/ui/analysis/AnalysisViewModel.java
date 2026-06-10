// ui/analysis/AnalysisViewModel.java
package com.example.plog.ui.analysis;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.plog.data.repository.AnalysisRepository;
import com.example.plog.data.repository.PhotoRepository;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.model.MonthlyReport;
import com.example.plog.util.SessionManager;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AnalysisViewModel extends AndroidViewModel {

    private final MutableLiveData<MonthlyReport> report    = new MutableLiveData<>();
    private final MutableLiveData<Boolean>       isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String>        error     = new MutableLiveData<>();

    // 지도용 위치 데이터
    private final LiveData<List<PhotoLocationDao.PhotoLocationWithImage>> locations;


    private final AnalysisRepository repository;
    private final PhotoRepository     photoRepository;
    private final int                 userId;
    private final ExecutorService     executor = Executors.newSingleThreadExecutor();

    public AnalysisViewModel(@NonNull Application application) {
        super(application);
        repository     = new AnalysisRepository(application);
        photoRepository = new PhotoRepository(application);
        userId         = (int) new SessionManager(application).getUserId();

        // 지도 위치 데이터 — 현재 월(일기 사진 추가일 기준)만 표시
        long[] range = currentMonthRange();
        locations = photoRepository.getMonthlyLocationsWithImage(userId, range[0], range[1]);
    }

    public LiveData<MonthlyReport>             getReport()    { return report;    }
    public LiveData<Boolean>                   getIsLoading() { return isLoading; }
    public LiveData<String>                    getError()     { return error;     }
    public LiveData<List<PhotoLocationDao.PhotoLocationWithImage>> getLocations() {
        return locations;
    }

    public void loadReport(int year, int month) {
        if (userId == -1) {
            error.setValue("로그인 정보가 없습니다.");
            return;
        }
        isLoading.setValue(true);
        executor.execute(() -> {
            try {
                // 기존 사진 중 location_name 없는 항목 역지오코딩 보정
                photoRepository.backfillLocationNames(getApplication());
                MonthlyReport result = repository.buildReport(userId, year, month);
                report.postValue(result);
            } catch (Exception e) {
                error.postValue("리포트 생성 실패: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    private static long[] currentMonthRange() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        return new long[]{ start.getTimeInMillis(), end.getTimeInMillis() };
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}