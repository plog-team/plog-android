package com.example.plog.network.dto;

import java.io.Serializable;
import java.util.List;

public class MatchRecommendResponse implements Serializable {
    private Long userId;
    private String nickname;
    private double similarityScore;
    private List<String> topCategories;

    public MatchRecommendResponse() {}

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public double getSimilarityScore() { return similarityScore; }
    public List<String> getTopCategories() { return topCategories; }
}