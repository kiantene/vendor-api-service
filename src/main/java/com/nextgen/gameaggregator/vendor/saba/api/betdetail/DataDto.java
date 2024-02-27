package com.nextgen.gameaggregator.vendor.saba.api.betdetail;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class DataDto {

    @SerializedName("last_version_key")
    private String lastVersionKey;
    @SerializedName("BetDetails")
    private List<BetDetailsDto> betDetails;
}
