package com.example.plog.api.model;

import java.util.List;

public class TourResponse {
    public Response response;
    public static class Response {
        public Body body;
    }
    public static class Body {
        public Items items;
        public int totalCount;
    }
    public static class Items {
        public List<TourItem> item;
    }
}