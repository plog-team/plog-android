package com.example.plog.network.dto;

import java.util.List;

public class ExchangeMatchListResponse {
    private boolean success;
    private List<ExchangeMatchResponse> data;

    public boolean isSuccess() { return success; }
    public List<ExchangeMatchResponse> getData() { return data; }
}