package com.nextgen.gameaggregator.vendor.marblex.api.gameurl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorGameUrlVo implements GameUrlVo {

    @JsonProperty("Data")
    private VendorGameUrlDataVo data;

    @Override
    public String getGameUrl() {
        return this.data.getGameUrl();
    }
}
