package com.nextgen.gameaggregator.vendor.groove.api.rollback;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.groove.response.CommonResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollbackResponse extends CommonResponse {

    private String accounttransactionid;
    private String apiversion;
    private BigDecimal balance;
    private BigDecimal bonus_balance;
    private BigDecimal real_balance;
    private Integer game_mode;
    private String order;
}
