package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.util.UuidUtil;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutProcessor implements CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> {

    public PromoPayoutProcessor() {

    }

    @Override
    public void process(PromoPayoutContext context) {
        context.setTransactionId(UuidUtil.newUuidV7StringRaw());
    }

    @Override
    public void onSuccess(PromoPayoutContext context, ClientBalanceResponse result) {

    }

    @Override
    public void onError(PromoPayoutContext context, Exception ex) {

    }
}
