package com.nextgen.gameaggregator.vendor.esoterica.api.result.betandresult;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("betAndResultServiceResponseMapperForResult")
public class BetAndResultServiceResponseMapper implements BetResultVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {

        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;

        return CommonResponse.builder()
                .transactionId(context.getTransactionId())
                .cash(balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .currency(context.getVendorCurrency())
                .error(ResponseCodes.SUCCESS.getCode())
                .description(ResponseCodes.SUCCESS.getMessage())
                .build();
    }
}
