package com.nextgen.gameaggregator.vendor.habanero.api.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.habanero.vo.StatusVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundTransferResponseVo {

    @JsonProperty("status")
    private StatusVo statusVo;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("currencycode")
    private String currencyCode;
}
