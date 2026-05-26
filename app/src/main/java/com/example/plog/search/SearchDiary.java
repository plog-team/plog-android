package com.example.plog.search;

/*
 * SearchDiary 클래스
 *
 * 검색 결과로 표시할 일기 데이터를 저장하는 모델(Model) 클래스
 * 하나의 객체가 일기 1개를 의미하며,
 * 날짜, 감정, 제목, 내용, 위치, 이미지 URL 정보를 포함한다.
 *
 * RecyclerView Adapter(SearchDiaryAdapter)가
 * 이 객체의 getter를 사용해 화면에 데이터를 출력한다.
 */
public class SearchDiary {

    private String date;
    private String emotion;
    private String title;
    private String content;
    private String location;
    private String imageUrl;

    // 생성자 : 객체 생성 시 모든 값 초기화
    public SearchDiary(String date, String emotion, String title,
                       String content, String location, String imageUrl) {
        this.date = date;
        this.emotion = emotion;
        this.title = title;
        this.content = content;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    // Getter : 저장된 데이터 반환
    public String getDate() { return date; }
    public String getEmotion() { return emotion; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
}