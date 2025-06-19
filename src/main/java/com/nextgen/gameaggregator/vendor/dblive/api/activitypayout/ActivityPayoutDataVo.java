package com.nextgen.gameaggregator.vendor.dblive.api.activitypayout;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityPayoutDataVo {
    private BigDecimal realAmount;
    private BigDecimal balance;
    private String loginName;
    private BigDecimal badAmount;
}
