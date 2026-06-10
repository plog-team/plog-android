package com.example.plog.util;

public class Constants {
    public static final String BASE_URL = "http://3.36.55.8:8080/";

    // Kakao REST API KEY
    public static final String KAKAO_REST_API_KEY = "52561ed4a8e63e6d5e5dcd340c4965bd";

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
