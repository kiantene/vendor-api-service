package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class GameUrlDto {
    private int action;
    private long ts;
    private String parent;
    private String uid;
    private BigDecimal balance;
    private String lang;
    private int gType;
    private String mType;
    private String windowMode;
}
