package com.nextgen.gameaggregator.vendor.pragmaticplay.api.refund;

import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class RefundVo extends ResponseVo {
    private String transactionId;   // Id of the transaction in wallet.
    private String currency;        // Currency of the player
    private BigDecimal cash;        // Real balance of the player
    private BigDecimal bonus;       // Bonus balance of the player
}
