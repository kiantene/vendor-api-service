package com.nextgen.gameaggregator.vendor.dblive.api.gameurl;

import lombok.Data;

@Data
public class GameUrlDto {
    private String loginName;
    private String loginPassword;
    private int deviceType;
    private int lang;
    private String backurl;
    private String gameTypeId;
    private long timestamp;
    private String playerLanguageV2;
}
