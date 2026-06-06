package com.example.plog.ui.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.repository.PhotoRepository;
import com.example.plog.util.SessionManager;

import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private final LiveData<List<PhotoLocationDao.PhotoLocationWithImage>> locations;

    public MapViewModel(@NonNull Application application) {
        super(application);
        int userId = new SessionManager(application).getUserId();
        PhotoRepository repository = new PhotoRepository(application);
        locations = repository.getAllLocationsWithImage(userId);  // ← 변경
    }

    public LiveData<List<PhotoLocationDao.PhotoLocationWithImage>> getLocations() {
        return locations;
    }
}