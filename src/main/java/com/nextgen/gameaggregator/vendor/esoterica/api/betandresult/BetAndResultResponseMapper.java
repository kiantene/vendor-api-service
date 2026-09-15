package com.nextgen.gameaggregator.vendor.esoterica.api.betandresult;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BetAndResultResponseMapper implements BetResultVendorResponseMapper<BetAndResultResponse> {
    @Override
    public BetAndResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {

        BigDecimal balance = balanceData != null ? balanceData.getBalance() : null;
        BigDecimal cash = balance != null ? balance.multiply(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE)).setScale(0, RoundingMode.DOWN) : BigDecimal.ZERO;

        return BetAndResultResponse.builder()
                .bet(CommonResponse.builder()
                        .transactionId(context.getTransactionId())
                        .cash(cash)
                        .bonus(BigDecimal.ZERO)
                        .currency(context.getVendorCurrency())
                        .usedPromo(BigDecimal.ZERO)
                        .error(ResponseCodes.SUCCESS.getCode())
                        .description(ResponseCodes.SUCCESS.getMessage())
                        .build())
                .win(CommonResponse.builder()
                        .transactionId(context.getTransactionId())
                        .cash(cash)
                        .bonus(BigDecimal.ZERO)
                        .currency(context.getVendorCurrency())
                        .error(ResponseCodes.SUCCESS.getCode())
                        .description(ResponseCodes.SUCCESS.getMessage())
                        .build())
                .error(ResponseCodes.SUCCESS.getCode())
                .description(ResponseCodes.SUCCESS.getMessage())
                .build();
    }
}
