package com.example.plog.util;

public class Constants {
    public static final String BASE_URL = "http://10.0.2.2:8080/";

    // Kakao REST API KEY
    public static final String KAKAO_REST_API_KEY = "";

    // OpenWeather API KEY
    public static final String OPENWEATHER_API_KEY = "";

    // Kakao 주소 변환 API URL
    public static final String KAKAO_COORD_TO_ADDRESS_URL =
            "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    // OpenWeather 현재 날씨 API URL
    public static final String OPENWEATHER_CURRENT_WEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final long TEMP_USER_ID = 1L;
    public static final long DEV_USER_ID = 1L;
}
