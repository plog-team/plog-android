package com.example.plog.network.dto;

public class ReportRequest {
    private Long reporterId;
    private Long reportedId;
    private String reason;

    public ReportRequest(Long reporterId, Long reportedId, String reason) {
        this.reporterId = reporterId;
        this.reportedId = reportedId;
        this.reason = reason;
    }
}