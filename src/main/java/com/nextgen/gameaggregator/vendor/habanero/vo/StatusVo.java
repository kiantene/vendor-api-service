package com.nextgen.gameaggregator.vendor.habanero.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusVo {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("autherror")
    private Boolean authError;

    @JsonProperty("nofunds")
    private Boolean noFunds;

    @JsonProperty("successdebit")
    private Boolean successDebit;

    @JsonProperty("successcredit")
    private Boolean successCredit;

    //invalid respond to trigger vendor resend
    @JsonProperty("retryStatus")
    private Boolean retryStatus;

    @JsonProperty("refundstatus")
    private Integer refundStatus;

    @JsonProperty("message")
    private String message;
}
