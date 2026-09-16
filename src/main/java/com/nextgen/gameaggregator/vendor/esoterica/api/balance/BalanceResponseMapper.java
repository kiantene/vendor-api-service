package com.nextgen.gameaggregator.vendor.esoterica.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {

        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        return CommonResponse.builder()
                .cash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .currency(context.getVendorCurrency())
                .error(ResponseCodes.SUCCESS.getCode())
                .description(ResponseCodes.SUCCESS.getMessage())
                .build();
    }
}
