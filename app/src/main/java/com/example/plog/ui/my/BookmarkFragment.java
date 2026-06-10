package com.example.plog.ui.my;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.plog.R;
import com.example.plog.api.model.BookmarkItem;
import com.example.plog.model.ApiResponse;
import com.example.plog.network.ApiClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookmarkFragment extends Fragment {

    private RecyclerView rvBookmarks;
    private ProgressBar progressBookmark;
    private View layoutEmpty;
    private BookmarkAdapter adapter;
    private final List<BookmarkItem> bookmarkList = new ArrayList<>();

    public static BookmarkFragment newInstance() {
        return new BookmarkFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookmark, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        rvBookmarks      = view.findViewById(R.id.rvBookmarks);
        progressBookmark = view.findViewById(R.id.progressBookmark);
        layoutEmpty      = view.findViewById(R.id.layoutEmpty);

        view.findViewById(R.id.ivBack).setOnClickListener(v ->
                requireActivity().onBackPressed()
        );

        adapter = new BookmarkAdapter(bookmarkList, this::removeBookmark);
        rvBookmarks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBookmarks.setAdapter(adapter);
        loadBookmarks();
    }

    private void loadBookmarks() {
        progressBookmark.setVisibility(View.VISIBLE);
        rvBookmarks.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        ApiClient.getApiService().getBookmarks()
                .enqueue(new Callback<ApiResponse<List<BookmarkItem>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<BookmarkItem>>> call,
                                           @NonNull Response<ApiResponse<List<BookmarkItem>>> response) {
                        if (!isAdded()) return;
                        progressBookmark.setVisibility(View.GONE);
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success) {
                            List<BookmarkItem> data = response.body().data;
                            bookmarkList.clear();
                            if (data != null) bookmarkList.addAll(data);
                            adapter.notifyDataSetChanged();
                            if (bookmarkList.isEmpty()) {
                                layoutEmpty.setVisibility(View.VISIBLE);
                            } else {
                                rvBookmarks.setVisibility(View.VISIBLE);
                            }
                        } else {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            Toast.makeText(requireContext(),
                                    "북마크를 불러오지 못했어요", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<BookmarkItem>>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBookmark.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(),
                                "네트워크 오류가 발생했어요", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeBookmark(BookmarkItem item, int position) {
        ApiClient.getApiService().removeBookmark(item.contentId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            adapter.removeAt(position);
                            if (bookmarkList.isEmpty()) {
                                rvBookmarks.setVisibility(View.GONE);
                                layoutEmpty.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(requireContext(),
                                    "북마크가 해제되었어요", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "북마크 해제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "네트워크 오류가 발생했어요", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
