package com.example.plog.api.model;

import com.google.gson.JsonElement;
import java.util.List;

public class TourResponse {
    public Response response;

    public static class Response {
        public Body body;
    }

    public static class Body {
        public int totalCount;

        public JsonElement items;
    }

    public static class Items {
        public List<TourItem> item;
    }
}