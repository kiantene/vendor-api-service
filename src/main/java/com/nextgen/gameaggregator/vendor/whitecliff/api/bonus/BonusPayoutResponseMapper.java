package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.whitecliff.vo.ResponseVo;
import org.springframework.stereotype.Component;

/**
 * Builds the WhiteCliff {@code /bonus} success body. Balance passes through unscaled — both the request
 * amount and the response balance are major-unit {@code BigDecimal}.
 */
@Component
public class BonusPayoutResponseMapper implements PromoPayoutVendorResponseMapper<ResponseVo> {

    @Override
    public ResponseVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        ResponseVo responseVo = new ResponseVo();
        responseVo.setBalance(balanceData.getBalance());
        responseVo.setStatus(ResponseCodes.SUCCESS);
        return responseVo;
    }
}
