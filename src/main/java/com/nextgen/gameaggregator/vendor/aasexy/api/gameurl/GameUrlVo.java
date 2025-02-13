package com.nextgen.gameaggregator.vendor.aasexy.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    private String status;

    private String url;
    

    @Override
    public String getGameUrl() {
        return url;
    }
}
