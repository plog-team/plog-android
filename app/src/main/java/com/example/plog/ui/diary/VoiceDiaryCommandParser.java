package com.example.plog.ui.diary;

public final class VoiceDiaryCommandParser {

    private VoiceDiaryCommandParser() {
    }

    public static Result parse(String spokenText) {
        if (spokenText == null || spokenText.trim().isEmpty()) return null;

        String normalized = spokenText.trim().replaceAll("\\s+", " ");
        Marker titleMarker = findMarker(normalized, 0, "제목은", "제목");
        if (titleMarker == null) return null;

        int titleStart = skipSpaces(normalized, titleMarker.end);
        Marker contentMarker = findMarker(normalized, titleStart, "내용은", "내용");
        if (contentMarker == null) return null;

        String title = normalized.substring(titleStart, contentMarker.start).trim();
        int bodyStart = skipSpaces(normalized, contentMarker.end);
        int bodyEnd = firstIndexOf(
                normalized,
                bodyStart,
                "전체 공개로",
                "전체 공개",
                "비공개로",
                "비공개",
                "비밀글로",
                "비밀글",
                "일기 저장",
                "저장해줘",
                "저장"
        );
        if (bodyEnd < 0) bodyEnd = normalized.length();

        String body = normalized.substring(bodyStart, bodyEnd).trim();
        if (title.isEmpty() || body.isEmpty()) return null;

        Boolean secret = null;
        if (normalized.contains("비공개") || normalized.contains("비밀글")) {
            secret = true;
        } else if (normalized.contains("전체 공개")) {
            secret = false;
        }

        return new Result(title, body, secret, normalized.contains("저장"));
    }

    private static Marker findMarker(String source, int fromIndex, String... markers) {
        int bestIndex = -1;
        String bestMarker = null;
        for (String marker : markers) {
            int index = source.indexOf(marker, fromIndex);
            if (index >= 0 && (bestIndex < 0 || index < bestIndex)) {
                bestIndex = index;
                bestMarker = marker;
            }
        }
        return bestMarker == null ? null : new Marker(bestIndex, bestIndex + bestMarker.length());
    }

    private static int firstIndexOf(String source, int fromIndex, String... markers) {
        Marker marker = findMarker(source, fromIndex, markers);
        return marker == null ? -1 : marker.start;
    }

    private static int skipSpaces(String source, int index) {
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        return index;
    }

    private static final class Marker {
        private final int start;
        private final int end;

        private Marker(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    public static final class Result {
        private final String title;
        private final String body;
        private final Boolean secret;
        private final boolean saveRequested;

        private Result(String title, String body, Boolean secret, boolean saveRequested) {
            this.title = title;
            this.body = body;
            this.secret = secret;
            this.saveRequested = saveRequested;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public Boolean getSecret() {
            return secret;
        }

        public boolean isSaveRequested() {
            return saveRequested;
        }
    }
}
