package com.example.plog.api.model;

import java.util.List;

public class TourDetailResponse {
    public Response response;

    public static class Response {
        public Body body;
    }
    public static class Body {
        public Items items;
    }
    public static class Items {
        public List<DetailItem> item;
    }
    public static class DetailItem {
        // detailCommon2 공통 필드
        public String contentid;
        public String title;
        public String addr1;
        public String firstimage;
        public String tel;
        public String homepage;
        public String overview;

        // detailIntro2 - 관광지(12)
        public String infocenter;       // 문의 및 안내
        public String usetime;          // 이용시간
        public String usefee;           // 이용요금
        public String restdate;         // 쉬는날
        public String parking;          // 주차
        public String opendate;         // 개장일

        // detailIntro2 - 문화시설(14)
        public String infocenterculture;  // 문의
        public String usetimeculture;     // 이용시간
        public String usefee_culture;     // 이용요금 (필드명 충돌 방지)
        public String restdateculture;    // 쉬는날
        public String parkingculture;     // 주차

        // detailIntro2 - 행사(15)
        public String eventstartdate;
        public String eventenddate;
        public String eventhomepage;
        public String sponsor1;
        public String playtime;
        public String usetimefestival;
        public String usefeefestival;
        public String eventstarttime; // 행사 시작 시간

        // detailIntro2 - 음식점(39)
        public String opentimefood;    // 영업시간
        public String restdatefood;    // 쉬는날
        public String parkingfood;     // 주차
        public String infocenterleports; // 레포츠(28) 문의

        // detailIntro2 - 레포츠(28)
        public String usetimeleports;
        public String usefeeleports;
        public String restdateleports;

        // 공통 getter: 카테고리에 상관없이 적절한 값 반환
        public String getUsetime() {
            if (notEmpty(usetime))         return usetime;
            if (notEmpty(usetimeculture))  return usetimeculture;
            if (notEmpty(usetimefestival)) return usetimefestival;
            if (notEmpty(opentimefood))    return opentimefood;
            if (notEmpty(usetimeleports))  return usetimeleports;
            return null;
        }
        public String getUsefee() {
            if (notEmpty(usefee))           return usefee;
            if (notEmpty(usefee_culture))   return usefee_culture;
            if (notEmpty(usefeefestival))   return usefeefestival;
            if (notEmpty(usefeeleports))    return usefeeleports;
            return null;
        }
        public String getRestdate() {
            if (notEmpty(restdate))         return restdate;
            if (notEmpty(restdateculture))  return restdateculture;
            if (notEmpty(restdatefood))     return restdatefood;
            if (notEmpty(restdateleports))  return restdateleports;
            return null;
        }
        public String getInfocenter() {
            if (notEmpty(infocenter))           return infocenter;
            if (notEmpty(infocenterculture))    return infocenterculture;
            if (notEmpty(infocenterleports))    return infocenterleports;
            return null;
        }
        private boolean notEmpty(String s) {
            return s != null && !s.isEmpty();
        }
    }
}
