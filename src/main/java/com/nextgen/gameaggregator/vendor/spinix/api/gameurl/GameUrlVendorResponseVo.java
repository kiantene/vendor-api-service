package com.nextgen.gameaggregator.vendor.spinix.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVendorResponseVo {
    private String reqId;
    private String status;
    private GameUrlVo data;
}
