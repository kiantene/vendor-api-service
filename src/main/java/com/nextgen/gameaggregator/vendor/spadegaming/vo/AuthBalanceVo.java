package com.nextgen.gameaggregator.vendor.spadegaming.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuthBalanceVo extends ResponseVo{
    private AcctInfoVo acctInfo;
}
