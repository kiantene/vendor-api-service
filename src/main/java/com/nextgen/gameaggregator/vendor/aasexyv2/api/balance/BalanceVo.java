package com.nextgen.gameaggregator.vendor.aasexyv2.api.balance;

import com.nextgen.gameaggregator.vendor.aasexyv2.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceVo extends ResponseVo {
    private String userId;
    private String balanceTs;
}
