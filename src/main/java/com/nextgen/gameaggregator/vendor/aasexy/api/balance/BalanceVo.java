package com.nextgen.gameaggregator.vendor.aasexy.api.balance;

import com.nextgen.gameaggregator.vendor.aasexy.vo.ResponseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceVo extends ResponseVo {
    private String userId;
    private String balanceTs;
}
