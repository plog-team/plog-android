package com.example.plog.network.dto;

import java.io.Serializable;
import java.util.List;

public class MatchRecommendResponse implements Serializable {
    public Long userId;
    public String nickname;
    public double similarityScore;
    public List<String> topCategories;

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public double getSimilarityScore() { return similarityScore; }
    public List<String> getTopCategories() { return topCategories; }
}