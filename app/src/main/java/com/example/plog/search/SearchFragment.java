package com.example.plog.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.plog.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SearchFragment extends Fragment {

    private RecyclerView rvSearchDiary;
    private SearchDiaryAdapter adapter;
    private ArrayList<SearchDiary> diaryList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        rvSearchDiary = view.findViewById(R.id.rvSearchDiary);

        diaryList = new ArrayList<>();

        adapter = new SearchDiaryAdapter(diaryList);

        rvSearchDiary.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchDiary.setAdapter(adapter);

        return view;
    }
}