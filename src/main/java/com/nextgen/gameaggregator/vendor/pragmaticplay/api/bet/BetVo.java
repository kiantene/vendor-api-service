package com.nextgen.gameaggregator.vendor.pragmaticplay.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class BetVo extends ResponseVo {
    private String transactionId;   // Id of the transaction in wallet.
    private String currency;        // Currency of the player
    private BigDecimal cash;        // Real balance of the player
    private BigDecimal bonus;       // Bonus balance of the player
    private BigDecimal usedPromo;   // Amount was used from the bonus balance.
}
