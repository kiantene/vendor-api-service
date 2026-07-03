package com.nextgen.gameaggregator.vendor.superbullgaming.api.promo;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.superbullgaming.api.betNSettle.BetNSettleDto;
import com.nextgen.gameaggregator.vendor.superbullgaming.config.SuperBullGamingConfig;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.superbullgaming.vo.CommonVo;
import org.springframework.stereotype.Component;

@Component
public class SBGPromoPayoutHandler extends AbstractPromoPayoutController<BetNSettleDto, PromoPayoutResponse> {

    public SBGPromoPayoutHandler(PromoPayoutRequestMapper requestMapper,
                                 PromoPayoutResponseMapper responseMapper,
                                 PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = SuperBullGamingConfig.CLASS_NAME)
    public CommonVo process(BetNSettleDto vendorRequest) {
        PromoPayoutResponse response = processRequest(vendorRequest);
        return mapToCommonVo(response);
    }

    public boolean isPromoPayout(BetNSettleDto dto) {
        return dto.getPromoType() != null && dto.getPromoType() != 0
                && dto.getPromoCode() != null && !dto.getPromoCode().isBlank();
    }

    private CommonVo mapToCommonVo(PromoPayoutResponse response) {
        CommonVo vo = new CommonVo();
        vo.setResponseCode(ResponseCode.SUCCESS);
        vo.setBalance(response.getBalance());
        vo.setCurrency(response.getCurrency());
        vo.setUsername(response.getUsername());
        return vo;
    }
}
