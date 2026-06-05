package com.example.plog.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
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

import android.graphics.Canvas;
import android.graphics.Paint;

public class MapOverlayManager {

    private static final String TAG = "MapOverlay";

    // ── 스타일 상수 ────────────────────────────────────────────────────────
    private static final int POLYLINE_COLOR = Color.parseColor("#FF4081");
    private static final float POLYLINE_WIDTH = 6f;

    private static final int CIRCLE_STROKE_COLOR = Color.parseColor("#1565C0");
    private static final int CIRCLE_FILL_COLOR = Color.parseColor("#2196F3");
    private static final float CIRCLE_STROKE_WIDTH = 3f;
    private static final float CIRCLE_FILL_ALPHA = 0.15f;

    private static final int CAMERA_PADDING_PX = 120;

    // 대한민국 중심 좌표
    private static final LatLng KOREA_CENTER = new LatLng(36.5, 127.5);
    private static final float KOREA_ZOOM = 6.5f;

    private final GoogleMap googleMap;
    private final Context context;

    private final List<Marker> markerList = new ArrayList<>();

    // bitmap cache
    private final LruCache<Integer, Bitmap> imageCache =
            new LruCache<Integer, Bitmap>(50) {

                @Override
                protected int sizeOf(
                        @NonNull Integer key,
                        @NonNull Bitmap value) {

                    return value.getByteCount() / 1024;
                }
            };

    private Polyline polyline;
    private Circle activityCircle;

    // ── 생성자 ─────────────────────────────────────────────────────────────
    public MapOverlayManager(@NonNull GoogleMap googleMap,
                             @NonNull Context context) {

        this.googleMap = googleMap;
        this.context = context;

        setupMarkerClickListener();
        setupInfoWindowAdapter();

        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        KOREA_CENTER,
                        KOREA_ZOOM
                )
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // renderAllWithImage
    // ────────────────────────────────────────────────────────────────────────
    public void renderAllWithImage(
            @NonNull List<PhotoLocationDao.PhotoLocationWithImage> locations) {

        Log.d(TAG, "renderAllWithImage 호출됨. 데이터 개수: " + locations.size());
        clearAll();

        if (locations.isEmpty()) {
            Log.w(TAG, "renderAllWithImage: 넘어온 locations 데이터가 비어있습니다.");
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            KOREA_CENTER,
                            KOREA_ZOOM
                    )
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
    // renderAll
    // ────────────────────────────────────────────────────────────────────────
    public void renderAll(
            @NonNull List<PhotoLocationEntity> locations) {

        Log.d(TAG, "renderAll 호출됨. 데이터 개수: " + locations.size());
        clearAll();

        if (locations.isEmpty()) {
            Log.w(TAG, "renderAll: 넘어온 locations 데이터가 비어있습니다.");
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            KOREA_CENTER,
                            KOREA_ZOOM
                    )
            );
            return;
        }

        List<LatLng> latLngList = toLatLngList(locations);

        addMarkers(locations, latLngList);
        addPolyline(latLngList);
        addActivityCircle(latLngList);

        moveCameraToFit(latLngList);
    }

    // ── InfoWindowAdapter ──────────────────────────────────────────────────
    private void setupInfoWindowAdapter() {

        googleMap.setInfoWindowAdapter(
                new GoogleMap.InfoWindowAdapter() {

                    @Nullable
                    @Override
                    public View getInfoWindow(@NonNull Marker marker) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public View getInfoContents(@NonNull Marker marker) {
                        Log.d(TAG, "getInfoContents 호출 (마커 말풍선 열림)");

                        Object tag = marker.getTag();

                        if (tag instanceof PhotoLocationDao.PhotoLocationWithImage) {
                            Log.d(TAG, "PhotoLocationWithImage 맞음");

                            return buildInfoWindowWithImage(
                                    (PhotoLocationDao.PhotoLocationWithImage) tag,
                                    marker
                            );
                        }
                        Log.d(TAG, "instanceof 실패");

                        return null;
                    }
                });
    }

    // ── LatLng 변환 ────────────────────────────────────────────────────────
    @NonNull
    private List<LatLng> toLatLngListFromWithImage(
            @NonNull List<PhotoLocationDao.PhotoLocationWithImage> locations) {

        List<LatLng> list = new ArrayList<>();

        for (PhotoLocationDao.PhotoLocationWithImage loc : locations) {
            list.add(new LatLng(loc.latitude, loc.longitude));
        }

        return list;
    }

    @NonNull
    private List<LatLng> toLatLngList(
            @NonNull List<PhotoLocationEntity> locations) {

        List<LatLng> list = new ArrayList<>();

        for (PhotoLocationEntity entity : locations) {
            list.add(new LatLng(entity.latitude, entity.longitude));
        }

        return list;
    }

    // ── 마커 추가 (이미지 포함) ──────────────────────────────────────────────
    private void addMarkersWithImage(
            @NonNull List<PhotoLocationDao.PhotoLocationWithImage> locations,
            @NonNull List<LatLng> latLngList) {

        Log.d(TAG, "addMarkersWithImage 내부 루프 시작 (순정 Canvas 원형 핀 적용)");

        for (int i = 0; i < locations.size(); i++) {
            PhotoLocationDao.PhotoLocationWithImage entity = locations.get(i);
            LatLng position = latLngList.get(i);

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(position)
                            .title(formatDate(entity.takenAt))
                            .snippet(buildSnippet(
                                    entity.locationName,
                                    entity.latitude,
                                    entity.longitude
                            ))
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    getMarkerHue(i, locations.size())
                            ))
                            .anchor(0.5f, 0.5f) // 원형 핀은 중심(0.5, 0.5)이 좌표에 오게 해야 자연스럽습니다.
            );

            if (marker != null) {
                marker.setTag(entity);
                markerList.add(marker);

                if (entity.imageUrl != null && !entity.imageUrl.isEmpty()) {

                    Log.d(TAG, "[" + i + "] 번째 마커 이미지 로드 시도: " + entity.imageUrl);

                    Glide.with(context)
                            .asBitmap()
                            .load(Uri.parse(entity.imageUrl))
                            .circleCrop() // 1. 👉 Glide 자체 기능으로 사진을 동그랗게 자릅니다.
                            .override(140, 140) // 핀 크기 지정
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                    // 테두리 두께 설정 (4dp를 픽셀로 변환)
                                    int borderWidthPx = (int) (4 * context.getResources().getDisplayMetrics().density);

                                    // 2. 👉 아래에 만든 헬퍼 메서드로 하얀 테두리를 입힙니다.
                                    Bitmap borderedBitmap = addBorderToCircularBitmap(resource, borderWidthPx, Color.WHITE);

                                    imageCache.put(entity.id, borderedBitmap);

                                    // 3. 👉 최종 테두리 원형 비트맵을 마커에 적용!
                                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(borderedBitmap));
                                }

                                @Override
                                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                            });
                }
            }
        }
    }

    // ── 마커 추가 (기본) ────────────────────────────────────────────────────
    private void addMarkers(
            @NonNull List<PhotoLocationEntity> locations,
            @NonNull List<LatLng> latLngList) {

        for (int i = 0; i < locations.size(); i++) {

            PhotoLocationEntity entity = locations.get(i);

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(latLngList.get(i))
                            .title(formatDate(entity.takenAt))
                            .snippet(buildSnippet(
                                    entity.locationName,
                                    entity.latitude,
                                    entity.longitude
                            ))
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    getMarkerHue(i, locations.size())
                            ))
                            .anchor(0.5f, 1.0f)
            );

            if (marker != null) {
                marker.setTag(entity);
                markerList.add(marker);
            }
        }
    }

    // ── InfoWindow 생성 ────────────────────────────────────────────────────
    @NonNull
    private View buildInfoWindowWithImage(
            @NonNull PhotoLocationDao.PhotoLocationWithImage entity,
            @NonNull Marker marker) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.marker_info_window, null);

        ImageView ivPhoto = view.findViewById(R.id.iv_marker_photo);
        TextView tvTitle = view.findViewById(R.id.tv_marker_title);
        TextView tvSnippet = view.findViewById(R.id.tv_marker_snippet);

        tvTitle.setText(formatDate(entity.takenAt));
        tvSnippet.setText(buildSnippet(
                entity.locationName,
                entity.latitude,
                entity.longitude
        ));

        // cache 사용
        Bitmap cachedBitmap = imageCache.get(entity.id);

        if (cachedBitmap != null) {
            Log.d(TAG, "InfoWindow: 캐시 이미지 사용");
            ivPhoto.setImageBitmap(cachedBitmap);
            return view;
        }

        ivPhoto.setImageResource(R.drawable.ic_photo_placeholder);

        if (entity.imageUrl != null && !entity.imageUrl.isEmpty()) {

            Log.d(TAG, "InfoWindow: 이미지 로드 시도 — " + entity.imageUrl);

            Glide.with(context)
                    .asBitmap()
                    .load(Uri.parse(entity.imageUrl))
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .centerCrop()
                    .into(new CustomTarget<Bitmap>() {

                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                            imageCache.put(entity.id, resource);
                            ivPhoto.setImageBitmap(resource);

                            // InfoWindow는 동적 업데이트가 안 되므로 닫았다가 다시 열어 갱신
                            if (marker.isInfoWindowShown()) {
                                marker.hideInfoWindow();
                                marker.showInfoWindow();
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                            ivPhoto.setImageResource(R.drawable.ic_photo_placeholder);
                        }
                    });

        } else {
            ivPhoto.setImageResource(R.drawable.ic_photo_placeholder);
        }

        return view;
    }

    // ── 마커 클릭 ──────────────────────────────────────────────────────────
    private void setupMarkerClickListener() {

        googleMap.setOnMarkerClickListener(marker -> {
            Log.d(TAG, "마커 클릭됨");
            marker.showInfoWindow();
            return false;
        });
    }

    // ── Polyline ───────────────────────────────────────────────────────────
    private void addPolyline(
            @NonNull List<LatLng> latLngList) {

        if (latLngList.size() < 2) return;

        polyline = googleMap.addPolyline(
                new PolylineOptions()
                        .addAll(latLngList)
                        .color(POLYLINE_COLOR)
                        .width(POLYLINE_WIDTH)
                        .geodesic(true)
        );
    }

    // ── Activity Circle ────────────────────────────────────────────────────
    private void addActivityCircle(
            @NonNull List<LatLng> latLngList) {

        if (latLngList.isEmpty()) return;

        LatLng center = calcCentroid(latLngList);

        double radius = Math.max(
                calcMaxRadius(center, latLngList),
                100.0
        );

        activityCircle = googleMap.addCircle(
                new CircleOptions()
                        .center(center)
                        .radius(radius)
                        .strokeColor(CIRCLE_STROKE_COLOR)
                        .strokeWidth(CIRCLE_STROKE_WIDTH)
                        .fillColor(applyAlpha(
                                CIRCLE_FILL_COLOR,
                                CIRCLE_FILL_ALPHA
                        ))
        );
    }

    // ── 카메라 이동 ────────────────────────────────────────────────────────
    private void moveCameraToFit(
            @NonNull List<LatLng> latLngList) {

        if (latLngList.isEmpty()) return;

        if (latLngList.size() == 1) {

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            latLngList.get(0),
                            15f
                    ),
                    800,
                    null
            );

            return;
        }

        googleMap.setOnMapLoadedCallback(() -> {

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (LatLng point : latLngList) {
                builder.include(point);
            }

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            builder.build(),
                            CAMERA_PADDING_PX
                    ),
                    1000,
                    null
            );
        });
    }

    // ── Toggle ─────────────────────────────────────────────────────────────
    public void setMarkersVisible(boolean visible) {
        for (Marker m : markerList) {
            m.setVisible(visible);
        }
    }

    public void setPolylineVisible(boolean visible) {
        if (polyline != null) {
            polyline.setVisible(visible);
        }
    }

    public void setCircleVisible(boolean visible) {
        if (activityCircle != null) {
            activityCircle.setVisible(visible);
        }
    }

    // ── Clear ──────────────────────────────────────────────────────────────
    public void clearAll() {

        for (Marker marker : markerList) {
            marker.hideInfoWindow();
            marker.remove();
        }

        markerList.clear();

        if (polyline != null) {
            polyline.remove();
            polyline = null;
        }

        if (activityCircle != null) {
            activityCircle.remove();
            activityCircle = null;
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private float getMarkerHue(int index, int total) {

        if (index == 0) {
            return BitmapDescriptorFactory.HUE_GREEN;
        }

        if (index == total - 1) {
            return BitmapDescriptorFactory.HUE_RED;
        }

        return BitmapDescriptorFactory.HUE_AZURE;
    }

    @NonNull
    private String buildSnippet(
            @Nullable String locationName,
            double lat,
            double lng) {

        return (locationName != null && !locationName.isEmpty())
                ? locationName
                : String.format(
                Locale.getDefault(),
                "%.5f, %.5f",
                lat,
                lng
        );
    }

    @NonNull
    private LatLng calcCentroid(
            @NonNull List<LatLng> points) {

        double sumLat = 0;
        double sumLng = 0;

        for (LatLng p : points) {

            sumLat += p.latitude;
            sumLng += p.longitude;
        }

        return new LatLng(
                sumLat / points.size(),
                sumLng / points.size()
        );
    }

    private double calcMaxRadius(
            @NonNull LatLng center,
            @NonNull List<LatLng> points) {

        float[] result = new float[1];

        double max = 0;

        for (LatLng p : points) {

            Location.distanceBetween(
                    center.latitude,
                    center.longitude,
                    p.latitude,
                    p.longitude,
                    result
            );

            if (result[0] > max) {
                max = result[0];
            }
        }

        return max;
    }

    private int applyAlpha(int color, float alpha) {

        int a = Math.round(255 * alpha);

        return Color.argb(
                a,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    @NonNull
    private String formatDate(long epochMs) {

        if (epochMs <= 0) {
            return "날짜 없음";
        }

        return new SimpleDateFormat(
                "MM/dd HH:mm",
                Locale.getDefault()
        ).format(new Date(epochMs));
    }
    // ── 외부 라이브러리 없이 동그란 사진에 테두리를 그려주는 헬퍼 메서드 ──
    private Bitmap addBorderToCircularBitmap(Bitmap src, int borderWidth, int borderColor) {
        int width = src.getWidth();
        int height = src.getHeight();

        // 원본과 동일한 크기의 빈 비트맵 생성
        Bitmap dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);

        // 1. 먼저 동그랗게 잘린 원본 이미지를 그립니다.
        canvas.drawBitmap(src, 0, 0, null);

        // 2. 그 위에 외곽선을 따라 지정한 색상의 링(테두리)을 그려줍니다.
        Paint paint = new Paint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE); // 채우기 없이 선만 그리기 위해 STROKE 지정
        paint.setStrokeWidth(borderWidth);
        paint.setAntiAlias(true); // 테두리를 부드럽게 처리

        float r = width / 2f;
        // 테두리 선이 비트맵 바깥 영역으로 삐져나가 잘리지 않도록, 두께의 절반만큼 안쪽으로 그려줍니다.
        canvas.drawCircle(r, r, r - (borderWidth / 2f), paint);

        return dst;
    }
}