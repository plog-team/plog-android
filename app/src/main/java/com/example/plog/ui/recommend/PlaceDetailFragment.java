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
import com.example.plog.api.model.BookmarkRequest;
import com.example.plog.api.model.PlaceDetailDto;
import com.example.plog.network.ApiClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceDetailFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_place_detail, container, false);
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

        ImageView ivImage     = view.findViewById(R.id.ivDetailImage);
        TextView tvTitle      = view.findViewById(R.id.tvDetailTitle);
        TextView tvCategory   = view.findViewById(R.id.tvDetailCategory);
        TextView tvAddress    = view.findViewById(R.id.tvDetailAddress);
        TextView tvDistance   = view.findViewById(R.id.tvDetailDistance);
        TextView tvTel        = view.findViewById(R.id.tvDetailTel);
        TextView tvUsetime    = view.findViewById(R.id.tvDetailUsetime);
        TextView tvUsefee     = view.findViewById(R.id.tvDetailUsefee);
        TextView tvRestdate   = view.findViewById(R.id.tvDetailRestdate);
        TextView tvOverview   = view.findViewById(R.id.tvDetailOverview);
        TextView tvEventDate  = view.findViewById(R.id.tvDetailEventDate);
        ProgressBar pbLoading = view.findViewById(R.id.pbDetailLoading);

        tvTitle.setText(title);
        tvCategory.setText(category);
        tvAddress.setText(address);
        tvDistance.setText("현재 위치에서 " + formatDist(distance));

        if (!imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(ivImage);
        } else {
            ivImage.setImageResource(R.drawable.gradient_diary_banner);
        }

        view.findViewById(R.id.btnBack)
                .setOnClickListener(v -> requireActivity().onBackPressed());

        view.findViewById(R.id.btnMap)
                .setOnClickListener(v -> {
                    Uri uri = Uri.parse("geo:" + lat + "," + lon
                            + "?q=" + lat + "," + lon + "(" + title + ")");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                });

        // 북마크
        view.findViewById(R.id.btnBookmark)
                .setOnClickListener(v -> {
                    BookmarkRequest req = new BookmarkRequest(
                            contentId, title, address, imageUrl, category, contentTypeId);
                    ApiClient.getApiService().addBookmark(req)
                            .enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(@NonNull Call<Void> call,
                                                       @NonNull Response<Void> resp) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        if (resp.isSuccessful()) {
                                            Toast.makeText(requireContext(),
                                                    title + " 북마크 추가됨",
                                                    Toast.LENGTH_SHORT).show();
                                        } else if (resp.code() == 400) {
                                            Toast.makeText(requireContext(),
                                                    "이미 북마크 추가된 항목입니다",
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(),
                                                    "북마크 실패: " + resp.code(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                @Override
                                public void onFailure(@NonNull Call<Void> call,
                                                      @NonNull Throwable t) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(),
                                                    "서버 연결 실패: " + t.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                                }
                            });
                });

        // 상세 정보 조회 (백엔드 경유)
        if (!contentId.isEmpty()) {
            pbLoading.setVisibility(View.VISIBLE);
            ApiClient.getApiService().getDetail(contentId, contentTypeId)
                    .enqueue(new Callback<PlaceDetailDto>() {
                        @Override
                        public void onResponse(@NonNull Call<PlaceDetailDto> call,
                                               @NonNull Response<PlaceDetailDto> resp) {
                            if (!isAdded()) return;
                            pbLoading.setVisibility(View.GONE);
                            if (!resp.isSuccessful() || resp.body() == null) return;

                            PlaceDetailDto d = resp.body();
                            requireActivity().runOnUiThread(() -> {
                                setOrHide(tvTel, "📞 ", d.tel);
                                if (d.overview != null && !d.overview.isEmpty()) {
                                    tvOverview.setVisibility(View.VISIBLE);
                                    tvOverview.setText(Html.fromHtml(
                                            d.overview, Html.FROM_HTML_MODE_COMPACT));
                                } else {
                                    tvOverview.setVisibility(View.GONE);
                                }
                                setOrHideHtml(tvUsetime, "🕐 ", d.usetime);
                                setOrHide(tvUsefee, "💰 ", d.usefee);
                                setOrHideHtml(tvRestdate, "🚫 ", d.restdate);
                                if (d.eventstartdate != null && !d.eventstartdate.isEmpty()) {
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
                        public void onFailure(@NonNull Call<PlaceDetailDto> call,
                                              @NonNull Throwable t) {
                            if (isAdded()) pbLoading.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void setOrHideHtml(TextView tv, String prefix, String value) {
        if (value != null && !value.isEmpty()) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(Html.fromHtml(prefix + value, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void setOrHide(TextView tv, String prefix, String value) {
        if (value != null && !value.isEmpty()) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(prefix + value);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.length() != 8) return raw;
        return raw.substring(0, 4) + "." + raw.substring(4, 6) + "." + raw.substring(6, 8);
    }

    private String formatDist(String dist) {
        try {
            double d = Double.parseDouble(dist);
            return d < 1000 ? (int) d + "m" : String.format("%.1fkm", d / 1000);
        } catch (Exception e) { return dist + "m"; }
    }
}