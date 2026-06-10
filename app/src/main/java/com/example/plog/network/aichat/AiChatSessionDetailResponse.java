package com.example.plog.network.aichat;

import java.util.List;

public class AiChatSessionDetailResponse {
    public DataWrapper data;

    public static class DataWrapper {
        public Detail data;
    }

    public static class Detail {
        public long sessionId;
        public String title;
        public String type;
        public String emotion;
        public List<Message> messages;
    }

    public static class Message {
        public String sender;   // "USER" or "AI"
        public String content;  // message 컬럼
    }
}