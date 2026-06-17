package com.nextgen.gameaggregator.vendor.egtdigital.api.balance;


import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import com.nextgen.gameaggregator.vendor.egtdigital.vo.ResponseCommonVo;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import org.springframework.stereotype.Component;


@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<ResponseCommonVo> {
    @Override
    public ResponseCommonVo toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return ResponseCommonVo.builder()
                .statusCode(ResponseCodes.OK.getCode())
                .balance(Amount.vendor(balanceData.getBalance()))
                .build();
    }
}
