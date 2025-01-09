package com.nextgen.gameaggregator.vendor.whitecliff.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo{

    private Integer status;

    @JsonProperty("user_id")
    private Integer userId;

    private String username;

    private String launch_url;

    @Override
    public String getGameUrl() {
        return this.launch_url;
    }

}
