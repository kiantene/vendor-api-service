package com.nextgen.gameaggregator.vendor.yeebet.api.betdetail;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class YeeBetBetDetailResponse {
    private int result;

    @SerializedName("array")
    private List<YeeBetDetailItem> betDetailArray;

    @Data
    public static class YeeBetDetailItem {

        @SerializedName("grurl")
        private String grUrl;
    }
}
