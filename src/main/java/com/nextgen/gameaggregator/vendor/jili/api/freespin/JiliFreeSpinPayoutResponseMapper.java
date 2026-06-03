package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.jili.api.bet.BetVo;
import org.springframework.stereotype.Component;

@Component
public class JiliFreeSpinPayoutResponseMapper implements PromoPayoutVendorResponseMapper<BetVo> {

    @Override
    public BetVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        BetVo betVo = new BetVo();
        betVo.setUsername(context.getVendorPlayerUsername());
        betVo.setCurrency(context.getVendorCurrency());
        betVo.setBalance(balanceData.getBalance());
        betVo.setToken(context.getToken());
        return betVo;
    }
}
