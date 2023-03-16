package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;


@Data
public class BalanceDataVo {

    private BalanceDataWalletVo wallet;

}