package com.nextgen.gameaggregator.vendor.pinnacle.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String loginUrl;
    private String userCode;
    private String loginId;
    private String token;
    private String updatedDate;

    @Override
    public String getGameUrl() {
        return loginUrl;
    }
}
