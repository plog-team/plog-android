package com.example.plog.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DiaryRepository {
    private static final String PREF_NAME = "diary_entries";

    private final SharedPreferences preferences;

    public DiaryRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public DiaryEntry getDiary(String dateKey) {
        String raw = preferences.getString(dateKey, null);
        if (raw == null) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(raw);
            DiaryEntry entry = new DiaryEntry();
            entry.setDate(json.optString("date", dateKey));
            entry.setTitle(json.optString("title", ""));
            entry.setBody(json.optString("body", ""));
            entry.setLocation(json.optString("location", ""));
            entry.setWeather(json.optString("weather", ""));
            entry.setSecret(json.optBoolean("secret", false));
            entry.setBookmarked(json.optBoolean("bookmarked", false));
            entry.setRepresentativePhotoIndex(json.optInt("representativePhotoIndex", 0));

            JSONArray photos = json.optJSONArray("photoUris");
            List<String> photoUris = new ArrayList<>();
            if (photos != null) {
                for (int i = 0; i < photos.length(); i++) {
                    photoUris.add(photos.optString(i));
                }
            }
            entry.setPhotoUris(photoUris);

            JSONArray galleryPhotos = json.optJSONArray("galleryPhotoUris");
            List<String> galleryPhotoUris = new ArrayList<>();
            if (galleryPhotos != null) {
                for (int i = 0; i < galleryPhotos.length(); i++) {
                    galleryPhotoUris.add(galleryPhotos.optString(i));
                }
            }
            entry.setGalleryPhotoUris(galleryPhotoUris);
            return entry;
        } catch (JSONException e) {
            return null;
        }
    }

    public List<DiaryEntry> getAllDiaries() {
        List<DiaryEntry> diaries = new ArrayList<>();

        for (String key : preferences.getAll().keySet()) {
            DiaryEntry diary = getDiary(key);
            if (diary != null) {
                diaries.add(diary);
            }
        }

        diaries.sort((d1, d2) -> d2.getDate().compareTo(d1.getDate()));

        return diaries;
    }

    public void saveDiary(DiaryEntry entry) {
        if (entry == null || entry.getDate() == null) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("date", entry.getDate());
            json.put("title", entry.getTitle());
            json.put("body", entry.getBody());
            json.put("location", entry.getLocation());
            json.put("weather", entry.getWeather());
            json.put("secret", entry.isSecret());
            json.put("bookmarked", entry.isBookmarked());
            json.put("representativePhotoIndex", entry.getRepresentativePhotoIndex());

            JSONArray photos = new JSONArray();
            for (String photoUri : entry.getPhotoUris()) {
                photos.put(photoUri);
            }
            json.put("photoUris", photos);

            JSONArray galleryPhotos = new JSONArray();
            if (entry.getGalleryPhotoUris() != null) {
                for (String uri : entry.getGalleryPhotoUris()) {
                    galleryPhotos.put(uri);
                }
            }
            json.put("galleryPhotoUris", galleryPhotos);

            preferences.edit()
                    .putString(entry.getDate(), json.toString())
                    .apply();
        } catch (JSONException ignored) {
        }
    }
}
