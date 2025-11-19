package com.nextgen.gameaggregator.vendor.jdb.api.promo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JdbPromoPayoutHandler extends AbstractPromoPayoutController<PromoPayoutRequest, PromoPayoutResponse> {

    public JdbPromoPayoutHandler(PromoPayoutRequestMapper requestMapper,
                                 PromoPayoutResponseMapper responseMapper,
                                 PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> handlePromo(Map<String, String> rawRequest) {
        ObjectMapper mapper = new ObjectMapper();
        PromoPayoutResponse response = processRequest(mapper.convertValue(rawRequest, PromoPayoutRequest.class));

        CommonVo vo = new CommonVo();
        vo.setBalance(response.getBalance());
        vo.setStatus(response.getStatus());
        vo.setErr_text(response.getErrText());
        return ResponseEntity.ok(vo);
    }
}
