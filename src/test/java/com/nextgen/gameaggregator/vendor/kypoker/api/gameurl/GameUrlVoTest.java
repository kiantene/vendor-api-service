package com.nextgen.gameaggregator.vendor.kypoker.api.gameurl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameUrlVoTest {
    @Test
    void getGameUrl_returnsUrlFromResponse() throws Exception {
        String json = "{\"d\":{\"url\":\"https://h5.ky34.com/index.html?account=10001_111111\"}}";
        GameUrlVo vo = new ObjectMapper().readValue(json, GameUrlVo.class);
        assertEquals("https://h5.ky34.com/index.html?account=10001_111111", vo.getGameUrl());
    }

    @Test
    void setGameUrl_updatesUnderlyingUrl() throws Exception {
        String json = "{\"d\":{\"url\":\"https://h5.ky34.com/index.html\"}}";
        GameUrlVo vo = new ObjectMapper().readValue(json, GameUrlVo.class);
        vo.setGameUrl("https://h5.ky34.com/index.html?jumpType=1");
        assertEquals("https://h5.ky34.com/index.html?jumpType=1", vo.getGameUrl());
    }
}