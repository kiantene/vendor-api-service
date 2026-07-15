package com.nextgen.gameaggregator.vendor.evolutionv2.api.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import org.springframework.stereotype.Component;

/**
 * Evolution v2 promo-payout integration.
 */
@Component
public class PromoPayoutResponseMapper implements PromoPayoutVendorResponseMapper<ResponseVo> {

    @Override
    public ResponseVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        ResponseVo response = new ResponseVo();
        response.setResponseCode(ResponseCode.OK);
        response.setBalance(balanceData.getBalance());
        if (context instanceof EvolutionPromoPayoutContext evolutionContext) {
            response.setUuid(evolutionContext.getVendorRequestUuid());
        }
        return response;
    }
}
