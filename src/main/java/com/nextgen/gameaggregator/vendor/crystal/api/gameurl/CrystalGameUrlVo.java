package com.nextgen.gameaggregator.vendor.crystal.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrystalGameUrlVo implements GameUrlVo {

    @JsonProperty("data")
    private GameDataDto data;

    @Override
    public String getGameUrl() {
        return this.data.getUrl();
    }
}
