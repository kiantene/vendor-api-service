package com.nextgen.gameaggregator.vendor.wmlive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WMGameUrlVo implements GameUrlVo {

    private String result;

    private String errorCode;

    private String errorMessage;


    @Override
    public String getGameUrl() {
        return this.result;
    }

}
