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
            return entry;
        } catch (JSONException e) {
            return null;
        }
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

            preferences.edit()
                    .putString(entry.getDate(), json.toString())
                    .apply();
        } catch (JSONException ignored) {
        }
    }
}
