// ui/map/MapFragment.java
package com.example.plog.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.plog.R;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.chip.Chip;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private MapViewModel      viewModel;
    private MapOverlayManager overlayManager;
    private ProgressBar       pbLoading;

    // ── PhotoLocationWithImage로 변경 ──────────────────────────────────────
    private List<PhotoLocationDao.PhotoLocationWithImage> pendingLocations;
    private boolean isMapReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pbLoading = view.findViewById(R.id.pb_loading);
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);
        setupChips(view);
        observeData();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // ── context 추가 ───────────────────────────────────────────────────
        overlayManager = new MapOverlayManager(googleMap, requireContext());
        isMapReady = true;

        if (pendingLocations != null) {
            overlayManager.renderAllWithImage(pendingLocations);  // ← 변경
            pendingLocations = null;
        }
    }

    private void observeData() {
        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            // null 체크만 진행합니다. 빈 리스트([])여도 아래로 내려가야 지도가 비워집니다.
            if (locations == null) {
                pbLoading.setVisibility(View.GONE);
                return;
            }

            pbLoading.setVisibility(View.GONE);

            if (isMapReady) {
                overlayManager.renderAllWithImage(locations);  // 이미지 핀으로 그리기
            } else {
                pendingLocations = locations;
            }
        });
    }

    private void setupChips(@NonNull View view) {
        ((Chip) view.findViewById(R.id.chip_markers)).setOnCheckedChangeListener(
                (btn, checked) -> {
                    if (overlayManager != null) overlayManager.setMarkersVisible(checked);
                });
        ((Chip) view.findViewById(R.id.chip_route)).setOnCheckedChangeListener(
                (btn, checked) -> {
                    if (overlayManager != null) overlayManager.setPolylineVisible(checked);
                });
        ((Chip) view.findViewById(R.id.chip_radius)).setOnCheckedChangeListener(
                (btn, checked) -> {
                    if (overlayManager != null) overlayManager.setCircleVisible(checked);
                });
    }
}