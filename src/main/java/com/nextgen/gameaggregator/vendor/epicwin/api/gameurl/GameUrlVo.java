package com.nextgen.gameaggregator.vendor.epicwin.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String Ticket;
    private Integer Status;
    private String Description;
    private String ResponseDateTime;
    private String Url;

    @Override
    public String getGameUrl() {
        return Url;
    }
}