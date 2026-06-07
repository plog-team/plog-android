package com.example.plog.network.dto;

public class BlockRequest {
    private Long blockerId;
    private Long blockedId;

    public BlockRequest(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }
}