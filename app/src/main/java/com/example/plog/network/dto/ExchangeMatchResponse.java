package com.example.plog.network.dto;

import java.util.List;

public class ExchangeMatchResponse {
    private Long id;
    private String status;
    private String createdAt;
    private String requesterNickname;
    private List<String> topCategories;

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getRequesterNickname() { return requesterNickname; }
    public List<String> getTopCategories() { return topCategories; }
}