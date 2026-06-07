package com.example.plog.api.model;

import java.util.List;

public class PreferenceResponse {
    public boolean success;
    public Data data;

    public static class Data {
        public Long userId;
        public List<String> preferredCategories;
        public String updatedAt;
    }
}