package com.example.plog.util;

import java.util.HashMap;
import java.util.Map;

public class LabelTranslator {

    private static final Map<String, String> LABEL_MAP = new HashMap<>();

    static {
        // ── 자연 / 날씨 / 계절 (★계절 및 풍경 대폭 추가) ──────────────────────────
        LABEL_MAP.put("sky", "하늘");
        LABEL_MAP.put("cloud", "구름");
        LABEL_MAP.put("sunset", "노을");
        LABEL_MAP.put("sunrise", "일출");
        LABEL_MAP.put("nature", "자연");
        LABEL_MAP.put("tree", "나무");
        LABEL_MAP.put("flower", "꽃");
        LABEL_MAP.put("plant", "식물");
        LABEL_MAP.put("grass", "잔디");
        LABEL_MAP.put("mountain", "산");
        LABEL_MAP.put("beach", "해변");
        LABEL_MAP.put("sea", "바다");
        LABEL_MAP.put("ocean", "바다");
        LABEL_MAP.put("river", "강");
        LABEL_MAP.put("lake", "호수");
        LABEL_MAP.put("forest", "숲");
        LABEL_MAP.put("woods", "숲");
        LABEL_MAP.put("field", "들판");
        LABEL_MAP.put("waterfall", "폭포");
        LABEL_MAP.put("snow", "눈");
        LABEL_MAP.put("rain", "비");
        LABEL_MAP.put("fog", "안개");
        LABEL_MAP.put("mist", "안개");
        LABEL_MAP.put("night", "밤");
        LABEL_MAP.put("moon", "달");
        LABEL_MAP.put("star", "별");
        LABEL_MAP.put("rainbow", "무지개");
        LABEL_MAP.put("leaf", "나뭇잎");
        LABEL_MAP.put("leaves", "나뭇잎");
        LABEL_MAP.put("cherry blossom", "벚꽃");
        LABEL_MAP.put("spring", "봄");
        LABEL_MAP.put("summer", "여름");
        LABEL_MAP.put("autumn", "가을");
        LABEL_MAP.put("fall", "가을");
        LABEL_MAP.put("winter", "겨울");

        // ── 동물 ────────────────────────────────────────────────────────────
        LABEL_MAP.put("dog", "강아지");
        LABEL_MAP.put("cat", "고양이");
        LABEL_MAP.put("bird", "새");
        LABEL_MAP.put("fish", "물고기");
        LABEL_MAP.put("animal", "동물");

        // ── 음식 / 디저트 / 음료 / 카페 소품 (★일상 먹방 보완) ───────────────────
        LABEL_MAP.put("food", "음식");
        LABEL_MAP.put("drink", "음료");
        LABEL_MAP.put("coffee", "커피");
        LABEL_MAP.put("bread", "빵");
        LABEL_MAP.put("cake", "케이크");
        LABEL_MAP.put("fruit", "과일");
        LABEL_MAP.put("vegetable", "채소");
        LABEL_MAP.put("pizza", "피자");
        LABEL_MAP.put("sushi", "초밥/스시");
        LABEL_MAP.put("noodle", "면 요리");
        LABEL_MAP.put("ramen", "라면/라멘");
        LABEL_MAP.put("rice", "밥");
        LABEL_MAP.put("meat", "고기");
        LABEL_MAP.put("seafood", "해산물");
        LABEL_MAP.put("soup", "국/스프");
        LABEL_MAP.put("snack", "간식/과자");
        LABEL_MAP.put("dessert", "디저트");
        LABEL_MAP.put("sweet", "디저트");
        LABEL_MAP.put("cookie", "쿠키");
        LABEL_MAP.put("waffle", "와플");
        LABEL_MAP.put("donut", "도넛");
        LABEL_MAP.put("macaron", "마카롱");
        LABEL_MAP.put("ice cream", "아이스크림");
        LABEL_MAP.put("brunch", "브런치");
        LABEL_MAP.put("salad", "샐러드");
        LABEL_MAP.put("pasta", "파스타");
        LABEL_MAP.put("burger", "햄버거");
        LABEL_MAP.put("hamburger", "햄버거");
        LABEL_MAP.put("sandwich", "샌드위치");
        LABEL_MAP.put("chicken", "치킨");
        LABEL_MAP.put("fast food", "패스트푸드");
        LABEL_MAP.put("beer", "맥주");
        LABEL_MAP.put("wine", "와인");
        LABEL_MAP.put("cocktail", "칵테일");
        LABEL_MAP.put("tea", "차(음료)");
        LABEL_MAP.put("cafele", "카페라떼");
        LABEL_MAP.put("cuisine", "요리");
        LABEL_MAP.put("dish", "요리");
        LABEL_MAP.put("plate", "접시");
        LABEL_MAP.put("tableware", "식기");
        LABEL_MAP.put("cup", "컵");
        LABEL_MAP.put("mug", "머그컵");
        LABEL_MAP.put("glass", "유리잔");
        LABEL_MAP.put("menu", "메뉴판");

        // ── 장소 / 인테리어 / 거리 풍경 ──────────────────────────────────────
        LABEL_MAP.put("cafe", "카페");
        LABEL_MAP.put("restaurant", "식당");
        LABEL_MAP.put("building", "건물");
        LABEL_MAP.put("city", "도시");
        LABEL_MAP.put("street", "거리");
        LABEL_MAP.put("road", "도로");
        LABEL_MAP.put("park", "공원");
        LABEL_MAP.put("school", "학교");
        LABEL_MAP.put("store", "가게");
        LABEL_MAP.put("room", "실내");
        LABEL_MAP.put("house", "집");
        LABEL_MAP.put("bridge", "다리");
        LABEL_MAP.put("night view", "야경");
        LABEL_MAP.put("interior design", "인테리어");
        LABEL_MAP.put("window", "창문");
        LABEL_MAP.put("architecture", "건축물");
        LABEL_MAP.put("facade", "건물 외관");
        LABEL_MAP.put("alley", "골목길");

        // ── 사람 / 일상 패션 ────────────────────────────────────────────────
        LABEL_MAP.put("person", "사람");
        LABEL_MAP.put("people", "사람들");
        LABEL_MAP.put("face", "얼굴");
        LABEL_MAP.put("smile", "미소");
        LABEL_MAP.put("baby", "아기");
        LABEL_MAP.put("child", "어린이");
        LABEL_MAP.put("selfie", "셀카");
        LABEL_MAP.put("human face", "얼굴");
        LABEL_MAP.put("mirror", "거울");
        LABEL_MAP.put("footwear", "신발");
        LABEL_MAP.put("shoes", "신발");
        LABEL_MAP.put("clothing", "패션");
        LABEL_MAP.put("outfit", "오오티디(OOTD)");
        LABEL_MAP.put("fashion", "패션");
        LABEL_MAP.put("hand", "손");
        LABEL_MAP.put("shadow", "그림자");
        LABEL_MAP.put("bag", "가방");
        LABEL_MAP.put("backpack", "배낭");
        LABEL_MAP.put("glasses", "안경");
        LABEL_MAP.put("sunglasses", "선글라스");

        // ── 예술 / 전시 / 시각 디자인 ──────────────────────────────────────
        LABEL_MAP.put("art", "예술");
        LABEL_MAP.put("painting", "그림");
        LABEL_MAP.put("drawing", "드로잉");
        LABEL_MAP.put("sketch", "스케치");
        LABEL_MAP.put("illustration", "일러스트");
        LABEL_MAP.put("artwork", "작품");
        LABEL_MAP.put("visual arts", "미술");
        LABEL_MAP.put("exhibition", "전시회");
        LABEL_MAP.put("museum", "박물관");
        LABEL_MAP.put("gallery", "갤러리");
        LABEL_MAP.put("sculpture", "조각상");
        LABEL_MAP.put("mural", "벽화");
        LABEL_MAP.put("poster", "포스터");
        LABEL_MAP.put("advertising", "광고");
        LABEL_MAP.put("billboard", "광고판");
        LABEL_MAP.put("sign", "간판/표지판");
        LABEL_MAP.put("signage", "간판");
        LABEL_MAP.put("graphic design", "그래픽 디자인");
        LABEL_MAP.put("font", "폰트/글꼴");
        LABEL_MAP.put("text", "텍스트/글자");
        LABEL_MAP.put("logo", "로고");

        // ── 사물 / 일상 소품 / 공부 (★기록 및 갓생 소품 추가) ─────────────────────
        LABEL_MAP.put("car", "자동차");
        LABEL_MAP.put("book", "책");
        LABEL_MAP.put("diary", "다이어리");
        LABEL_MAP.put("journal", "일기/기록");
        LABEL_MAP.put("stationery", "문구류");
        LABEL_MAP.put("pen", "펜");
        LABEL_MAP.put("phone", "핸드폰");
        LABEL_MAP.put("computer", "컴퓨터");
        LABEL_MAP.put("laptop", "노트북");
        LABEL_MAP.put("tablet", "태블릿");
        LABEL_MAP.put("monitor", "모니터");
        LABEL_MAP.put("desk", "책상");
        LABEL_MAP.put("bicycle", "자전거");
        LABEL_MAP.put("train", "기차");
        LABEL_MAP.put("bus", "버스");
        LABEL_MAP.put("airplane", "비행기");
        LABEL_MAP.put("boat", "배");
        LABEL_MAP.put("flower bouquet", "꽃다발");
        LABEL_MAP.put("bouquet", "꽃다발");
        LABEL_MAP.put("lighting", "조명");
        LABEL_MAP.put("lamp", "조명");
        LABEL_MAP.put("candle", "양초");
        LABEL_MAP.put("ticket", "티켓");
        LABEL_MAP.put("receipt", "영수증");
        LABEL_MAP.put("toy", "장난감");
        LABEL_MAP.put("doll", "인형");

        // ── 활동 / 취미 / 운동 (★다이어리에 단골로 나오는 취미 대폭 보완) ───────────
        LABEL_MAP.put("sport", "스포츠");
        LABEL_MAP.put("fitness", "운동");
        LABEL_MAP.put("running", "러닝");
        LABEL_MAP.put("gym", "헬스장");
        LABEL_MAP.put("yoga", "요가");
        LABEL_MAP.put("pilates", "필라테스");
        LABEL_MAP.put("hiking", "등산");
        LABEL_MAP.put("climbing", "클라이밍");
        LABEL_MAP.put("swimming", "수영");
        LABEL_MAP.put("golf", "골프");
        LABEL_MAP.put("tennis", "테니스");
        LABEL_MAP.put("cycling", "라이딩");
        LABEL_MAP.put("camping", "캠핑");
        LABEL_MAP.put("tent", "텐트");
        LABEL_MAP.put("picnic", "피크닉");
        LABEL_MAP.put("travel", "여행");
        LABEL_MAP.put("excursion", "나들이");
        LABEL_MAP.put("cooking", "요리하기");
        LABEL_MAP.put("baking", "베이킹");
        LABEL_MAP.put("gardening", "식물 가꾸기");
        LABEL_MAP.put("game", "게임");
        LABEL_MAP.put("gaming", "게임");
        LABEL_MAP.put("movie", "영화");
        LABEL_MAP.put("cinema", "영화관");
        LABEL_MAP.put("theater", "공연장/극장");
        LABEL_MAP.put("music", "음악");
        LABEL_MAP.put("concert", "공연");
        LABEL_MAP.put("festival", "축제");
        LABEL_MAP.put("guitar", "기타(악기)");
        LABEL_MAP.put("piano", "피아노");
        LABEL_MAP.put("studying", "공부");
        LABEL_MAP.put("work", "일/작업");
    }

    /**
     * 영어 라벨 → 한국어 변환
     * 매핑 없으면 원본 영어 그대로 반환
     */
    public static String translate(String englishLabel) {
        if (englishLabel == null) return "";
        String lower = englishLabel.toLowerCase().trim();
        return LABEL_MAP.getOrDefault(lower, englishLabel);
    }
}