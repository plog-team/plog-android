package com.example.plog.network.dto;

import java.util.List;

public class MatchRecommendListResponse {
    private boolean success;
    private List<MatchRecommendResponse> data;

    public boolean isSuccess() { return success; }
    public List<MatchRecommendResponse> getData() { return data; }
}