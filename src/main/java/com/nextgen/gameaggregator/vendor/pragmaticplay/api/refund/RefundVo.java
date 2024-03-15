package com.nextgen.gameaggregator.vendor.pragmaticplay.api.refund;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundVo extends ResponseVo {
    private String transactionId;   // Id of the refund transaction in Casino Operator system.
}
