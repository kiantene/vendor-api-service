package com.nextgen.gameaggregator.vendor.pragmaticplay.api.refund;

import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class RefundVo extends ResponseVo {
    private String transactionId;   // Id of the refund transaction in Casino Operator system.
}
