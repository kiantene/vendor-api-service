package com.nextgen.gameaggregator.vendor.pragmaticplay.api.adjustment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdjustmentVo extends ResponseVo {
    private String transactionId;   // Id of the refund transaction in Casino Operator system.
    private String currency;        // Currency of the player
    private BigDecimal cash;        // Real balance of the player
    private BigDecimal bonus;       // Bonus balance of the player
}
