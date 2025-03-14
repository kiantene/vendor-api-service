package com.nextgen.gameaggregator.vendor.yesbingo.api.gameurl;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.gson.annotations.SerializedName;
import jakarta.annotation.Nullable;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameUrlVo implements com.nextgen.gameaggregator.operator.game.url.GameUrlVo {

    private String status;

    @Nullable
    private String path;

    @Nullable
    @SerializedName("err_text")
    private String errText;

    @Override
    public String getGameUrl() {
        return this.path;
    }
}
