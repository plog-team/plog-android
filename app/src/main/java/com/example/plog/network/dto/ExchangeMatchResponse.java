package com.example.plog.network.dto;

import java.util.List;

public class ExchangeMatchResponse {
    private boolean success;
    private Data data;

    public Long getId() { return data != null ? data.id : null; }
    public String getStatus() { return data != null ? data.status : null; }
    public String getCreatedAt() { return data != null ? data.createdAt : null; }
    public String getRequesterNickname() { return data != null ? data.requesterNickname : null; }
    public List<String> getTopCategories() { return data != null ? data.topCategories : null; }
    public Long getPartnerUserId() { return data != null ? data.partnerUserId : null; }

    public static class Data {
        private Long id;
        private String status;
        private String createdAt;
        private String requesterNickname;
        private List<String> topCategories;
        private Long partnerUserId;
    }
}