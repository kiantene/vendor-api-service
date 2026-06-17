package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * GA-14464: CQ9 can reply HTTP 200 with no {@code data} (e.g. its outage body
 * {@code {"data":null,"status":{...}}}). getGameUrl() must return null in that case so
 * BaseGameUrlService raises InvalidVendorResponseException("cannot get game url") rather than NPE.
 */
class GameUrlVendorResponseVoTest {

    private static GameUrlVo data(String url) {
        GameUrlVo d = new GameUrlVo();
        d.setUrl(url);
        return d;
    }

    @Test
    void getGameUrl_returnsNull_whenDataMissing() {
        GameUrlVendorResponseVo vo = new GameUrlVendorResponseVo();
        vo.setData(null); // CQ9 outage: HTTP 200, no data object
        assertNull(vo.getGameUrl(), "missing data must yield null, not NPE");
    }

    @Test
    void getGameUrl_returnsNull_whenUrlMissing() {
        GameUrlVendorResponseVo vo = new GameUrlVendorResponseVo();
        vo.setData(data(null));
        assertNull(vo.getGameUrl(), "missing url must yield null");
    }

    @Test
    void getGameUrl_returnsUrl_whenNoLeaveUrl() {
        GameUrlVendorResponseVo vo = new GameUrlVendorResponseVo();
        vo.setData(data("https://play.cq9/game"));
        assertEquals("https://play.cq9/game", vo.getGameUrl());
    }

    @Test
    void getGameUrl_ignoresEmptyLeaveUrl() {
        // Regression for the corrected ternary: empty leaveUrl must not append "&leaveUrl=null".
        GameUrlVendorResponseVo vo = new GameUrlVendorResponseVo();
        vo.setData(data("https://play.cq9/game"));
        vo.setLeaveUrl("");
        assertEquals("https://play.cq9/game", vo.getGameUrl());
    }

    @Test
    void getGameUrl_appendsLeaveUrl_whenPresent() {
        GameUrlVendorResponseVo vo = new GameUrlVendorResponseVo();
        vo.setData(data("https://play.cq9/game"));
        vo.setLeaveUrl("https://gogoroyal.com");
        assertEquals("https://play.cq9/game&leaveUrl=https://gogoroyal.com", vo.getGameUrl());
    }
}
