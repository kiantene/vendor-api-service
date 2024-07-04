package com.nextgen.gameaggregator.vendor.epicwin.api.jackpot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.epicwin.vo.ResponseVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JackpotVo extends ResponseVo {
    @JsonProperty("OldBalance")
    private BigDecimal oldBalance;
    @JsonProperty("NewBalance")
    private BigDecimal newBalance;
}

