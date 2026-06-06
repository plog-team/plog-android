package com.example.plog.network.dto;

public class ExchangeSessionResponse {
    private Long id;
    private Long exchangeRoomId;
    private String status;
    private String startDate;
    private String endDate;

    public Long getId() { return id; }
    public Long getExchangeRoomId() { return exchangeRoomId; }
    public String getStatus() { return status; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
}