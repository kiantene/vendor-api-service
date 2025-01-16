package com.nextgen.gameaggregator.vendor.koolbet.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KBGameUrlVo implements GameUrlVo {


    private String data;

    private String errorCode;

    private String message;

    @Override
    public String getGameUrl() {
        return this.getData();
    }
}
