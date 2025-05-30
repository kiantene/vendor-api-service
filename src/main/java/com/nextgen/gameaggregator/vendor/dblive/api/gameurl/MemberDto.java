package com.nextgen.gameaggregator.vendor.dblive.api.gameurl;

import lombok.Data;

@Data
public class MemberDto {
    private String loginName;
    private String loginPassword;
    private int lang;
    private long timestamp;
}
