package com.example.plog.ui.diary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VoiceDiaryCommandParserTest {

    @Test
    public void parsesPublicDiarySaveCommand() {
        VoiceDiaryCommandParser.Result result = VoiceDiaryCommandParser.parse(
                "제목은 첫 번째 편지 내용은 첫번째 테스트 편지 입니다 행복한 하루 보내세요 전체 공개로 일기 저장"
        );

        assertNotNull(result);
        assertEquals("첫 번째 편지", result.getTitle());
        assertEquals("첫번째 테스트 편지 입니다 행복한 하루 보내세요", result.getBody());
        assertFalse(result.getSecret());
        assertTrue(result.isSaveRequested());
    }

    @Test
    public void parsesSecretDiaryWithoutSaveCommand() {
        VoiceDiaryCommandParser.Result result = VoiceDiaryCommandParser.parse(
                "제목은 비밀 일기 내용은 오늘 있었던 일을 기록합니다 비밀글로 작성"
        );

        assertNotNull(result);
        assertEquals("비밀 일기", result.getTitle());
        assertEquals("오늘 있었던 일을 기록합니다", result.getBody());
        assertTrue(result.getSecret());
        assertFalse(result.isSaveRequested());
    }

    @Test
    public void rejectsCommandWithoutContentMarker() {
        assertNull(VoiceDiaryCommandParser.parse("제목은 첫 번째 편지 저장"));
    }
}
