package com.example.plog.network.aichat;

import java.util.List;

public class AiChatSessionListResponse {
    public DataWrapper data;

    public static class DataWrapper {
        public List<Session> data;
    }

    public static class Session {
        public long sessionId;
        public String title;
        public String type;
        public String emotion;
        public boolean isDiary;
        public String createdAt;
    }
}