package com.example.plog.network.dto;

public class ExchangeSessionResponse {
    private boolean success;
    private Data data;

    public Long getId() { return data != null ? data.id : null; }
    public Long getExchangeRoomId() { return data != null ? data.exchangeRoomId : null; }
    public String getStatus() { return data != null ? data.status : null; }
    public String getStartDate() { return data != null ? data.startDate : null; }
    public String getEndDate() { return data != null ? data.endDate : null; }

    public static class Data {
        private Long id;
        private Long exchangeRoomId;
        private String status;
        private String startDate;
        private String endDate;
        private boolean extended;
    }
}