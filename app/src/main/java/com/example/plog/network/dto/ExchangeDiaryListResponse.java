package com.example.plog.network.dto;

import java.util.List;

public class ExchangeDiaryListResponse {
    private boolean success;
    private List<ExchangeDiaryResponse.Data> data;

    public boolean isSuccess() { return success; }
    public List<ExchangeDiaryResponse.Data> getData() { return data; }
}