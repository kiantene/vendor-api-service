package com.nextgen.gameaggregator.vendor.hacksawgaming.api.gameurl;

import lombok.Data;

@Data
public class GameUrlVendorResponseVo {
    private GameUrlVo data;
    private Integer code;
    private String msg;
}
