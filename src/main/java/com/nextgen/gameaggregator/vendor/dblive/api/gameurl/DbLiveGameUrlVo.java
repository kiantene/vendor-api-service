package com.nextgen.gameaggregator.vendor.dblive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbLiveGameUrlVo implements GameUrlVo {
    private String code;
    private String message;
    private Data data;

    @Override
    public String getGameUrl() {
        return this.data.url;
    }

    @lombok.Data
    public static class Data {
        private String url;
    }
}
