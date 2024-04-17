package com.nextgen.gameaggregator.vendor.saba.api.gameurl;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {
    @SerializedName("error_code")
    private Integer errorCode;

    @SerializedName("message")
    private String message;

    @NotNull(message = "Data can not be blank")
    @SerializedName("Data")
    private String data;

    @Override
    public String getGameUrl() {
        return this.data;
    }
}
