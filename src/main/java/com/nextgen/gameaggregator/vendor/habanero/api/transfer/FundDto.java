package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundDto {

    @NotNull
    @JsonProperty("debitandcredit")
    public Boolean debitAndCredit;

    @JsonProperty("fundinfo")
    public FundInfoDto fundInfoDto[];

    @JsonProperty("refund")
    public RefundDto refundDto;
}
