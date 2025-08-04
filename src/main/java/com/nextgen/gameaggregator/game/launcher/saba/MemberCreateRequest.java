package com.nextgen.gameaggregator.game.launcher.saba;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberCreateRequest {

    @JsonProperty("vendor_id")
    private String vendorId;

    @JsonProperty("vendor_member_id")
    private String vendorMemberId;

    @JsonProperty("operatorid")
    private String operatorId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("oddstype")
    private String oddsType;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("mintransfer")
    private String minTransfer;

    @JsonProperty("maxtransfer")
    private String maxTransfer;
}
