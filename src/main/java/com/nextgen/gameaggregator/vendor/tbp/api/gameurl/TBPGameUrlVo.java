package com.nextgen.gameaggregator.vendor.tbp.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TBPGameUrlVo implements GameUrlVo {
    private Data data;
    private String gameUrl;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String sessionId;
    }
}
