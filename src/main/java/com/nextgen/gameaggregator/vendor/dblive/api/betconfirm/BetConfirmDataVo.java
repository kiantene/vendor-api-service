package com.nextgen.gameaggregator.vendor.dblive.api.betconfirm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BetConfirmDataVo {
    private BigDecimal balance;
    private String loginName;
    private List<BetInfoDto> realBetInfo;
    private BigDecimal realBetAmount;

}
