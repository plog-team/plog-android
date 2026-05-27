package com.example.plog.ui.recommend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.api.TourApiClient;
import com.example.plog.api.model.TourDetailResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceDetailFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(
                R.layout.fragment_place_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        String contentId     = args.getString("contentId", "");
        String contentTypeId = args.getString("contentTypeId", "");
        String title         = args.getString("title", "");
        String address       = args.getString("address", "");
        String imageUrl      = args.getString("imageUrl", "");
        String category      = args.getString("category", "");
        String distance      = args.getString("distance", "");
        float  lat           = args.getFloat("latitude");
        float  lon           = args.getFloat("longitude");

        // Views
        ImageView ivImage       = view.findViewById(R.id.ivDetailImage);
        TextView tvTitle        = view.findViewById(R.id.tvDetailTitle);
        TextView tvCategory     = view.findViewById(R.id.tvDetailCategory);
        TextView tvAddress      = view.findViewById(R.id.tvDetailAddress);
        TextView tvDistance     = view.findViewById(R.id.tvDetailDistance);
        TextView tvTel          = view.findViewById(R.id.tvDetailTel);
        TextView tvUsetime      = view.findViewById(R.id.tvDetailUsetime);
        TextView tvUsefee       = view.findViewById(R.id.tvDetailUsefee);
        TextView tvRestdate     = view.findViewById(R.id.tvDetailRestdate);
        TextView tvOverview     = view.findViewById(R.id.tvDetailOverview);
        TextView tvEventDate    = view.findViewById(R.id.tvDetailEventDate);
        ProgressBar pbLoading   = view.findViewById(R.id.pbDetailLoading);

        // 기본 정보 먼저 세팅
        tvTitle.setText(title);
        tvCategory.setText(category);
        tvAddress.setText(address);
        tvDistance.setText("현재 위치에서 " + formatDist(distance));

        if (!imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(ivImage);
        } else {
            ivImage.setImageResource(R.drawable.gradient_diary_banner);
        }

        // 뒤로가기
        view.findViewById(R.id.btnBack)
                .setOnClickListener(v -> requireActivity().onBackPressed());

        // 지도에서 보기
        view.findViewById(R.id.btnMap)
                .setOnClickListener(v -> {
                    Uri uri = Uri.parse("geo:" + lat + "," + lon
                            + "?q=" + lat + "," + lon
                            + "(" + title + ")");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });

        // 북마크
        view.findViewById(R.id.btnBookmark)
                .setOnClickListener(v ->
                        Toast.makeText(requireContext(),
                                title + " 북마크 추가됨",
                                Toast.LENGTH_SHORT).show());

        // 상세 정보 API 호출
        if (!contentId.isEmpty()) {
            pbLoading.setVisibility(View.VISIBLE);

            // 1단계: detailCommon2 → tel, overview
            TourApiClient.getTourService().getDetailWithType(
                            TourApiClient.TOUR_API_KEY,
                            contentId,
                            "Plog", "AND", "json")
                    .enqueue(new Callback<TourDetailResponse>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<TourDetailResponse> call,
                                @NonNull Response<TourDetailResponse> resp) {
                            if (!isAdded()) return;

                            if (resp.isSuccessful()
                                    && resp.body() != null
                                    && resp.body().response != null
                                    && resp.body().response.body != null
                                    && resp.body().response.body.items != null
                                    && resp.body().response.body.items.item != null
                                    && !resp.body().response.body.items.item.isEmpty()) {

                                TourDetailResponse.DetailItem d =
                                        resp.body().response.body.items.item.get(0);

                                requireActivity().runOnUiThread(() -> {
                                    setOrHide(tvTel, "📞 ", d.tel);
                                    if (d.overview != null && !d.overview.isEmpty()) {
                                        tvOverview.setVisibility(View.VISIBLE);
                                        tvOverview.setText(
                                                Html.fromHtml(d.overview,
                                                        Html.FROM_HTML_MODE_COMPACT));
                                    } else {
                                        tvOverview.setVisibility(View.GONE);
                                    }
                                });
                            }

                            // 2단계: detailIntro2 → usetime, usefee, restdate, 행사일정
                            if (!contentTypeId.isEmpty()) {
                                TourApiClient.getTourService().getDetailIntro(
                                                TourApiClient.TOUR_API_KEY,
                                                contentId, contentTypeId,
                                                "Plog", "AND", "json")
                                        .enqueue(new Callback<TourDetailResponse>() {
                                            @Override
                                            public void onResponse(
                                                    @NonNull Call<TourDetailResponse> call,
                                                    @NonNull Response<TourDetailResponse> resp) {
                                                if (!isAdded()) return;
                                                pbLoading.setVisibility(View.GONE);

                                                if (!resp.isSuccessful()
                                                        || resp.body() == null
                                                        || resp.body().response == null
                                                        || resp.body().response.body == null
                                                        || resp.body().response.body.items == null
                                                        || resp.body().response.body.items.item == null
                                                        || resp.body().response.body.items.item.isEmpty())
                                                    return;

                                                TourDetailResponse.DetailItem d =
                                                        resp.body().response.body.items.item.get(0);

                                                requireActivity().runOnUiThread(() -> {
                                                    setOrHide(tvUsetime,  "🕐 ", d.getUsetime());
                                                    setOrHide(tvUsefee,   "💰 ", d.getUsefee());
                                                    setOrHide(tvRestdate, "🚫 ", d.getRestdate());

                                                    if (d.eventstartdate != null
                                                            && !d.eventstartdate.isEmpty()) {
                                                        tvEventDate.setVisibility(View.VISIBLE);
                                                        tvEventDate.setText("📅 "
                                                                + formatDate(d.eventstartdate)
                                                                + " ~ "
                                                                + formatDate(d.eventenddate));
                                                    } else {
                                                        tvEventDate.setVisibility(View.GONE);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure(
                                                    @NonNull Call<TourDetailResponse> call,
                                                    @NonNull Throwable t) {
                                                if (isAdded())
                                                    pbLoading.setVisibility(View.GONE);
                                            }
                                        });
                            } else {
                                requireActivity().runOnUiThread(() ->
                                        pbLoading.setVisibility(View.GONE));
                            }
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<TourDetailResponse> call,
                                @NonNull Throwable t) {
                            if (isAdded())
                                pbLoading.setVisibility(View.GONE);
                        }
                    });
        }
    }

    // 값 있으면 보여주고 없으면 숨기기
    private void setOrHide(TextView tv, String prefix, String value) {
        if (value != null && !value.isEmpty()) {
            tv.setVisibility(View.VISIBLE);

            String combinedText = prefix + value;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                tv.setText(Html.fromHtml(combinedText, Html.FROM_HTML_MODE_COMPACT));
            } else {
                tv.setText(Html.fromHtml(combinedText));
            }
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    // 날짜 포맷 20250101 → 2025.01.01
    private String formatDate(String raw) {
        if (raw == null || raw.length() != 8) return raw;
        return raw.substring(0, 4) + "."
                + raw.substring(4, 6) + "."
                + raw.substring(6, 8);
    }

    private String formatDist(String dist) {
        try {
            double d = Double.parseDouble(dist);
            return d < 1000 ? (int) d + "m"
                    : String.format("%.1fkm", d / 1000);
        } catch (Exception e) { return dist + "m"; }
    }
}
