package com.example.plog.api.model;

import java.util.List;

public class SeoulResponse {
    public SeoulCityData CITYDATA;

    public static class SeoulCityData {
        public String AREA_NM;
        public List<Population> LIVE_PPLTN_STTS;

        public static class Population {
            public String AREA_CONGEST_LVL;  // 여유/보통/붐빔/매우붐빔
            public String AREA_PPLTN_MIN;
            public String AREA_PPLTN_MAX;
            public String PPLTN_TIME;
        }
    }
}