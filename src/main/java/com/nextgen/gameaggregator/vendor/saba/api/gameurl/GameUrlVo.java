package com.nextgen.gameaggregator.vendor.saba.api.gameurl;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @SerializedName("error_code")
    private Integer errorCode;

    @SerializedName("message")
    private String message;

    @SerializedName("Data")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
