// ui/analysis/AnalysisFragment.java
package com.example.plog.ui.analysis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plog.R;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.ui.map.MapOverlayManager;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Calendar;
import java.util.List;

public class AnalysisFragment extends Fragment implements OnMapReadyCallback {

    private AnalysisViewModel viewModel;
    private ReportAdapter     adapter;
    private MapOverlayManager overlayManager;

    // 지도 준비 전 데이터 도착 시 저장
    private List<PhotoLocationDao.PhotoLocationWithImage> pendingLocationsWithImage;
    private boolean isMapReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv        = view.findViewById(R.id.rv_report);
        ProgressBar  pbLoading = view.findViewById(R.id.pb_loading);
        ImageButton  btnBack   = view.findViewById(R.id.btn_back);

        // ── 뒤로가기 ───────────────────────────────────────────────────────
        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // ── 지도 Fragment 추가 ─────────────────────────────────────────────
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        // ── RecyclerView 설정 ──────────────────────────────────────────────
        adapter = new ReportAdapter();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // ── ViewModel ─────────────────────────────────────────────────────
        viewModel = new ViewModelProvider(this).get(AnalysisViewModel.class);

        Calendar now = Calendar.getInstance();
        viewModel.loadReport(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1
        );

        // ── 리포트 관찰 ────────────────────────────────────────────────────
        viewModel.getReport().observe(getViewLifecycleOwner(), report -> {
            if (report != null) {
                adapter.submitReport(report);
            }
        });

        // ── 지도 위치 데이터 관찰 ──────────────────────────────────────────
        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            if (locations == null || locations.isEmpty()) return;
            if (isMapReady) {
                overlayManager.renderAllWithImage(locations);
            } else {
                pendingLocationsWithImage = locations;
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE)
        );

        viewModel.getError().observe(getViewLifecycleOwner(), errMsg -> {
            if (errMsg != null)
                Toast.makeText(requireContext(), errMsg, Toast.LENGTH_SHORT).show();
        });
    }

    // ── 지도 준비 완료 ─────────────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        googleMap.setOnCameraMoveStartedListener(reason -> {
            requireView().findViewById(R.id.map_container)
                    .getParent().requestDisallowInterceptTouchEvent(true);
        });

        overlayManager = new MapOverlayManager(googleMap, requireContext());
        isMapReady = true;

        if (pendingLocationsWithImage != null) {
            overlayManager.renderAllWithImage(pendingLocationsWithImage);
            pendingLocationsWithImage = null;
        }
    }
}