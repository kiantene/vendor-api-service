package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class GameUrlServiceTest {
    private final GameUrlService service = new GameUrlService();

    @Test
    void onResponseSuccess_appendsJumpTypeWithAmpersand_whenUrlAlreadyHasQueryParams() {
        GameUrlVo vo = buildVoWithUrl("https://h5.ky34.com/index.html?account=10001_111111&token=FBE54A7273EE4F15B363C3F98F32B19F&lang=zh-CN&KindID=0");
        GameUrlVo result = service.onResponseSuccess(vo, null);
        assertEquals(
                "https://h5.ky34.com/index.html?account=10001_111111&token=FBE54A7273EE4F15B363C3F98F32B19F&lang=zh-CN&KindID=0&jumpType=1",
                result.getGameUrl()
        );
    }

    @Test
    void onResponseSuccess_appendsJumpTypeWithQuestionMark_whenUrlHasNoQueryParams() {
        GameUrlVo vo = buildVoWithUrl("https://h5.ky34.com/index.html");
        GameUrlVo result = service.onResponseSuccess(vo, null);
        assertEquals("https://h5.ky34.com/index.html?jumpType=1", result.getGameUrl());
    }

    @Test
    void onResponseSuccess_doesNothing_whenUrlIsNull() {
        GameUrlVo vo = buildVoWithUrl(null);
        GameUrlVo result = service.onResponseSuccess(vo, null);
        assertNull(result.getGameUrl());
    }

    private GameUrlVo buildVoWithUrl(String url) {
        String json = url == null
                ? "{\"d\":{\"url\":null}}"
                : "{\"d\":{\"url\":\"" + url.replace("\"", "\\\"") + "\"}}";
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, GameUrlVo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}