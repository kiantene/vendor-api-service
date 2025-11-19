package com.nextgen.gameaggregator.vendor.mg.api.promo;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.mg.api.betresult.UpdateBalanceDto;
import com.nextgen.gameaggregator.vendor.mg.api.betresult.UpdateBalanceVo;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutHandler extends AbstractPromoPayoutController<UpdateBalanceDto, PromoPayoutResponse> {

    public PromoPayoutHandler(PromoPayoutRequestMapper requestMapper,
                                 PromoPayoutResponseMapper responseMapper,
                                 PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<UpdateBalanceVo> promo(UpdateBalanceDto request) {
        PromoPayoutResponse response = processRequest(request);
        UpdateBalanceVo balanceVo = new UpdateBalanceVo();
        balanceVo.setCurrency(response.getCurrency());
        balanceVo.setBalance(response.getBalance());
        return ResponseEntity.ok(balanceVo);
    }
}
