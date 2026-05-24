// ui/map/MapOverlayManager.java
package com.example.plog.ui.map;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.data.db.dao.PhotoLocationDao;
import com.example.plog.data.db.entity.PhotoLocationEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapOverlayManager {

    // ── 스타일 상수 ────────────────────────────────────────────────────────
    private static final int   POLYLINE_COLOR      = Color.parseColor("#FF4081");
    private static final float POLYLINE_WIDTH      = 6f;
    private static final int   CIRCLE_STROKE_COLOR = Color.parseColor("#1565C0");
    private static final int   CIRCLE_FILL_COLOR   = Color.parseColor("#2196F3");
    private static final float CIRCLE_STROKE_WIDTH = 3f;
    private static final float CIRCLE_FILL_ALPHA   = 0.15f;
    private static final int   CAMERA_PADDING_PX   = 120;

    // 대한민국 중심 좌표
    private static final LatLng KOREA_CENTER = new LatLng(36.5, 127.5);
    private static final float  KOREA_ZOOM   = 6.5f;

    private final GoogleMap    googleMap;
    private final Context      context;
    private final List<Marker> markerList = new ArrayList<>();
    private Polyline polyline;
    private Circle   activityCircle;

    // ── 생성자 ─────────────────────────────────────────────────────────────
    public MapOverlayManager(@NonNull GoogleMap googleMap,
                             @NonNull Context context) {
        this.googleMap = googleMap;
        this.context   = context;
        setupMarkerClickListener();
        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(KOREA_CENTER, KOREA_ZOOM)
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // renderAllWithImage — 사진 포함 버전 (메인)
    // ────────────────────────────────────────────────────────────────────────
    public void renderAllWithImage(
            @NonNull List<PhotoLocationDao.PhotoLocationWithImage> locations) {
        clearAll();

        if (locations.isEmpty()) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(KOREA_CENTER, KOREA_ZOOM)
            );
            return;
        }

        List<LatLng> latLngList = toLatLngListFromWithImage(locations);

        addMarkersWithImage(locations, latLngList);
        addPolyline(latLngList);
        addActivityCircle(latLngList);
        moveCameraToFit(latLngList);
    }

    // ────────────────────────────────────────────────────────────────────────
    // renderAll — 사진 없는 버전 (fallback)
    // ────────────────────────────────────────────────────────────────────────
    public void renderAll(@NonNull List<PhotoLocationEntity> locations) {
        clearAll();

        if (locations.isEmpty()) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(KOREA_CENTER, KOREA_ZOOM)
            );
            return;
        }

        List<LatLng> latLngList = toLatLngList(locations);

        addMarkers(locations, latLngList);
        addPolyline(latLngList);
        addActivityCircle(latLngList);
        moveCameraToFit(latLngList);
    }

    // ── LatLng 변환 (PhotoLocationWithImage) ──────────────────────────────
    @NonNull
    private List<LatLng> toLatLngListFromWithImage(
            @NonNull List<PhotoLocationDao.PhotoLocationWithImage> locations) {
        List<LatLng> list = new ArrayList<>();
        for (PhotoLocationDao.PhotoLocationWithImage loc : locations) {
            list.add(new LatLng(loc.latitude, loc.longitude));
        }
        return list;
    }

    // ── LatLng 변환 (PhotoLocationEntity) ─────────────────────────────────
    @NonNull
    private List<LatLng> toLatLngList(
            @NonNull List<PhotoLocationEntity> locations) {
        List<LatLng> list = new ArrayList<>();
        for (PhotoLocationEntity entity : locations) {
            list.add(new LatLng(entity.latitude, entity.longitude));
        }
        return list;
    }

    // ── 마커 추가 (PhotoLocationWithImage) ────────────────────────────────
    private void addMarkersWithImage(
            @NonNull List<PhotoLocationDao.PhotoLocationWithImage> locations,
            @NonNull List<LatLng> latLngList) {

        for (int i = 0; i < locations.size(); i++) {
            PhotoLocationDao.PhotoLocationWithImage entity = locations.get(i);
            LatLng position = latLngList.get(i);

            float hue = getMarkerHue(i, locations.size());

            String title   = formatDate(entity.takenAt);
            String snippet = buildSnippet(entity.locationName,
                    entity.latitude, entity.longitude);

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
                    .anchor(0.5f, 1.0f));

            if (marker != null) {
                marker.setTag(entity);
                markerList.add(marker);
            }
        }

        // InfoWindow — 사진 포함
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Nullable
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }

            @Nullable
            @Override
            public View getInfoContents(@NonNull Marker marker) {
                if (!(marker.getTag() instanceof
                        PhotoLocationDao.PhotoLocationWithImage)) return null;
                return buildInfoWindowWithImage(
                        (PhotoLocationDao.PhotoLocationWithImage) marker.getTag());
            }
        });
    }

    // ── 마커 추가 (PhotoLocationEntity) ───────────────────────────────────
    private void addMarkers(@NonNull List<PhotoLocationEntity> locations,
                            @NonNull List<LatLng> latLngList) {
        for (int i = 0; i < locations.size(); i++) {
            PhotoLocationEntity entity   = locations.get(i);
            LatLng              position = latLngList.get(i);

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(formatDate(entity.takenAt))
                    .snippet(buildSnippet(entity.locationName,
                            entity.latitude, entity.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            getMarkerHue(i, locations.size())))
                    .anchor(0.5f, 1.0f));

            if (marker != null) {
                marker.setTag(entity);
                markerList.add(marker);
            }
        }
    }

    // ── InfoWindow 뷰 생성 (사진 포함) ────────────────────────────────────
    @NonNull
    private View buildInfoWindowWithImage(
            @NonNull PhotoLocationDao.PhotoLocationWithImage entity) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.marker_info_window, null);

        ImageView ivPhoto   = view.findViewById(R.id.iv_marker_photo);
        TextView  tvTitle   = view.findViewById(R.id.tv_marker_title);
        TextView  tvSnippet = view.findViewById(R.id.tv_marker_snippet);

        tvTitle.setText(formatDate(entity.takenAt));
        tvSnippet.setText(buildSnippet(entity.locationName,
                entity.latitude, entity.longitude));

        if (entity.imageUrl != null && !entity.imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(Uri.parse(entity.imageUrl))
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .centerCrop()
                    .into(ivPhoto);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_photo_placeholder);
        }

        return view;
    }

    // ── 마커 클릭 ──────────────────────────────────────────────────────────
    private void setupMarkerClickListener() {
        googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });
    }

    // ── Polyline (이동 경로) ───────────────────────────────────────────────
    private void addPolyline(@NonNull List<LatLng> latLngList) {
        if (latLngList.size() < 2) return;

        polyline = googleMap.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .geodesic(true)
                .clickable(true));
    }

    // ── 활동 반경 원 ───────────────────────────────────────────────────────
    private void addActivityCircle(@NonNull List<LatLng> latLngList) {
        if (latLngList.isEmpty()) return;

        LatLng center = calcCentroid(latLngList);
        double radius = Math.max(calcMaxRadius(center, latLngList), 100.0);

        activityCircle = googleMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeColor(CIRCLE_STROKE_COLOR)
                .strokeWidth(CIRCLE_STROKE_WIDTH)
                .fillColor(applyAlpha(CIRCLE_FILL_COLOR, CIRCLE_FILL_ALPHA)));
    }

    // ── 카메라 이동 ────────────────────────────────────────────────────────
    private void moveCameraToFit(@NonNull List<LatLng> latLngList) {
        if (latLngList.isEmpty()) return;

        if (latLngList.size() == 1) {
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 15f),
                    800, null
            );
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : latLngList) builder.include(point);
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(builder.build(), CAMERA_PADDING_PX),
                1000, null
        );
    }

    // ── 토글 ───────────────────────────────────────────────────────────────
    public void setMarkersVisible(boolean visible) {
        for (Marker m : markerList) m.setVisible(visible);
    }

    public void setPolylineVisible(boolean visible) {
        if (polyline != null) polyline.setVisible(visible);
    }

    public void setCircleVisible(boolean visible) {
        if (activityCircle != null) activityCircle.setVisible(visible);
    }

    // ── 전체 제거 ──────────────────────────────────────────────────────────
    public void clearAll() {
        for (Marker m : markerList) m.remove();
        markerList.clear();
        if (polyline != null)       { polyline.remove();       polyline = null; }
        if (activityCircle != null) { activityCircle.remove(); activityCircle = null; }
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────────────
    private float getMarkerHue(int index, int total) {
        if (index == 0)          return BitmapDescriptorFactory.HUE_GREEN;
        if (index == total - 1)  return BitmapDescriptorFactory.HUE_RED;
        return BitmapDescriptorFactory.HUE_AZURE;
    }

    @NonNull
    private String buildSnippet(@Nullable String locationName,
                                double lat, double lng) {
        return (locationName != null && !locationName.isEmpty())
                ? locationName
                : String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
    }

    @NonNull
    private LatLng calcCentroid(@NonNull List<LatLng> points) {
        double sumLat = 0, sumLng = 0;
        for (LatLng p : points) { sumLat += p.latitude; sumLng += p.longitude; }
        return new LatLng(sumLat / points.size(), sumLng / points.size());
    }

    private double calcMaxRadius(@NonNull LatLng center,
                                 @NonNull List<LatLng> points) {
        float[] result = new float[1];
        double  max    = 0;
        for (LatLng p : points) {
            Location.distanceBetween(
                    center.latitude, center.longitude,
                    p.latitude, p.longitude, result);
            if (result[0] > max) max = result[0];
        }
        return max;
    }

    private int applyAlpha(int color, float alpha) {
        int a = Math.round(255 * alpha);
        return Color.argb(a,
                Color.red(color), Color.green(color), Color.blue(color));
    }

    @NonNull
    private String formatDate(long epochMs) {
        if (epochMs <= 0) return "날짜 없음";
        return new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                .format(new Date(epochMs));
    }
}