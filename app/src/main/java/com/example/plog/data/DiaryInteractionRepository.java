package com.example.plog.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiaryInteractionRepository {
    private static final String PREF_NAME = "diary_interactions";
    private static final String COMMENT_PREFIX = "comments_";
    private static final String DECORATION_PREFIX = "decorations_";

    private final SharedPreferences preferences;

    public DiaryInteractionRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<DiaryLineComment> getComments(String diaryDate, int lineIndex) {
        List<DiaryLineComment> comments = new ArrayList<>();
        JSONArray array = readArray(COMMENT_PREFIX + diaryDate);
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.optJSONObject(i);
            if (json == null || json.optInt("lineIndex") != lineIndex) {
                continue;
            }
            comments.add(parseComment(json, diaryDate));
        }
        return comments;
    }

    public int getCommentCount(String diaryDate, int lineIndex) {
        return getComments(diaryDate, lineIndex).size();
    }

    public void addComment(String diaryDate, int lineIndex, String content) {
        JSONArray array = readArray(COMMENT_PREFIX + diaryDate);
        JSONObject json = new JSONObject();
        try {
            json.put("id", UUID.randomUUID().toString());
            json.put("diaryDate", diaryDate);
            json.put("lineIndex", lineIndex);
            json.put("authorName", "나");
            json.put("content", content);
            json.put("createdAt", System.currentTimeMillis());
            array.put(json);
            writeArray(COMMENT_PREFIX + diaryDate, array);
        } catch (JSONException ignored) {
        }
    }

    public void deleteComment(String diaryDate, String commentId) {
        JSONArray oldArray = readArray(COMMENT_PREFIX + diaryDate);
        JSONArray newArray = new JSONArray();
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject json = oldArray.optJSONObject(i);
            if (json != null && !commentId.equals(json.optString("id"))) {
                newArray.put(json);
            }
        }
        writeArray(COMMENT_PREFIX + diaryDate, newArray);
    }

    public void updateComment(String diaryDate, String commentId, String content) {
        JSONArray oldArray = readArray(COMMENT_PREFIX + diaryDate);
        JSONArray newArray = new JSONArray();
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject json = oldArray.optJSONObject(i);
            if (json == null) {
                continue;
            }
            if (commentId.equals(json.optString("id"))) {
                try {
                    json.put("content", content);
                } catch (JSONException ignored) {
                }
            }
            newArray.put(json);
        }
        writeArray(COMMENT_PREFIX + diaryDate, newArray);
    }

    public List<DiaryEmojiDecoration> getDecorations(String diaryDate) {
        List<DiaryEmojiDecoration> decorations = new ArrayList<>();
        JSONArray array = readArray(DECORATION_PREFIX + diaryDate);
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.optJSONObject(i);
            if (json != null) {
                decorations.add(parseDecoration(json, diaryDate));
            }
        }
        return decorations;
    }

    public DiaryEmojiDecoration addDecoration(String diaryDate, String emoji) {
        DiaryEmojiDecoration decoration = new DiaryEmojiDecoration();
        decoration.setId(UUID.randomUUID().toString());
        decoration.setDiaryDate(diaryDate);
        decoration.setAuthorName("나");
        decoration.setEmoji(emoji);
        decoration.setXRatio(0.45f);
        decoration.setYRatio(0.25f);
        decoration.setScale(1.0f);
        decoration.setCreatedAt(System.currentTimeMillis());
        saveDecoration(decoration);
        return decoration;
    }

    public void saveDecoration(DiaryEmojiDecoration decoration) {
        JSONArray oldArray = readArray(DECORATION_PREFIX + decoration.getDiaryDate());
        JSONArray newArray = new JSONArray();
        boolean replaced = false;
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject json = oldArray.optJSONObject(i);
            if (json == null) {
                continue;
            }
            if (decoration.getId().equals(json.optString("id"))) {
                newArray.put(toJson(decoration));
                replaced = true;
            } else {
                newArray.put(json);
            }
        }
        if (!replaced) {
            newArray.put(toJson(decoration));
        }
        writeArray(DECORATION_PREFIX + decoration.getDiaryDate(), newArray);
    }

    public void deleteDecoration(String diaryDate, String decorationId) {
        JSONArray oldArray = readArray(DECORATION_PREFIX + diaryDate);
        JSONArray newArray = new JSONArray();
        for (int i = 0; i < oldArray.length(); i++) {
            JSONObject json = oldArray.optJSONObject(i);
            if (json != null && !decorationId.equals(json.optString("id"))) {
                newArray.put(json);
            }
        }
        writeArray(DECORATION_PREFIX + diaryDate, newArray);
    }

    private DiaryLineComment parseComment(JSONObject json, String diaryDate) {
        DiaryLineComment comment = new DiaryLineComment();
        comment.setId(json.optString("id"));
        comment.setDiaryDate(json.optString("diaryDate", diaryDate));
        comment.setLineIndex(json.optInt("lineIndex"));
        comment.setAuthorName(json.optString("authorName", "나"));
        comment.setContent(json.optString("content"));
        comment.setCreatedAt(json.optLong("createdAt"));
        return comment;
    }

    private DiaryEmojiDecoration parseDecoration(JSONObject json, String diaryDate) {
        DiaryEmojiDecoration decoration = new DiaryEmojiDecoration();
        decoration.setId(json.optString("id"));
        decoration.setDiaryDate(json.optString("diaryDate", diaryDate));
        decoration.setAuthorName(json.optString("authorName", "나"));
        decoration.setEmoji(json.optString("emoji"));
        decoration.setXRatio((float) json.optDouble("xRatio", 0.45));
        decoration.setYRatio((float) json.optDouble("yRatio", 0.25));
        decoration.setScale((float) json.optDouble("scale", 1.0));
        decoration.setCreatedAt(json.optLong("createdAt"));
        return decoration;
    }

    private JSONObject toJson(DiaryEmojiDecoration decoration) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", decoration.getId());
            json.put("diaryDate", decoration.getDiaryDate());
            json.put("authorName", decoration.getAuthorName());
            json.put("emoji", decoration.getEmoji());
            json.put("xRatio", decoration.getXRatio());
            json.put("yRatio", decoration.getYRatio());
            json.put("scale", decoration.getScale());
            json.put("createdAt", decoration.getCreatedAt());
        } catch (JSONException ignored) {
        }
        return json;
    }

    private JSONArray readArray(String key) {
        try {
            return new JSONArray(preferences.getString(key, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void writeArray(String key, JSONArray array) {
        preferences.edit().putString(key, array.toString()).apply();
    }
}
