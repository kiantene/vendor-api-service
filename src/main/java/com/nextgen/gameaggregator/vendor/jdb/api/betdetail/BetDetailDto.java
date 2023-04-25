package com.nextgen.gameaggregator.vendor.jdb.api.betdetail;

import lombok.Data;

@Data
public class BetDetailDto {
    private int action;
    private long ts;
    private String parent;
    private String uid;
    private String lang;
    private int gType;
    private String seqNo;
    private int showUid;
}
