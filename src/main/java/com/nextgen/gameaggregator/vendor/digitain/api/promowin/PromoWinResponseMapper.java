package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/**
 * Builds the Digitain {@code /promowin} success body.
 *
 * <p>{@code bln} is the player balance after the payout, which the spec makes mandatory on a response.
 * {@code PromoPayoutProcessor} has already converted it to vendor view, so it passes through unscaled
 * apart from the 4dp Digitain expects.
 */
@Component
public class PromoWinResponseMapper implements PromoPayoutVendorResponseMapper<PromoWinResponse> {

    @Override
    public PromoWinResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return PromoWinResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .txid(context.getVendorTransactionId())
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .pid(balanceData.getUsername())
                .build();
    }
}
