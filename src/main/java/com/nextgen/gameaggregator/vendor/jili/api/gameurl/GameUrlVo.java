package com.nextgen.gameaggregator.vendor.jili.api.gameurl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    @JsonProperty("ErrorCode")
    private Integer errorCode;
    @JsonProperty("Message")
    private String message;
    @NotBlank(message = "url can not be blank")
    @JsonProperty("Data")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
