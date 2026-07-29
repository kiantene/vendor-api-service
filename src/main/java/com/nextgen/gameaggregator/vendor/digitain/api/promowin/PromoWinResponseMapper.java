package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class PromoWinResponseMapper implements BetResultVendorResponseMapper<PromoWinResponse> {
    @Override
    public PromoWinResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return PromoWinResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .txid(context.getVendorBetId())
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .pid(balanceData.getUsername())
                .build();
    }
}