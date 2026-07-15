package com.nextgen.gameaggregator.vendor.habanero.api.bonus;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.habanero.api.transfer.TransferVo;
import com.nextgen.gameaggregator.vendor.habanero.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class HabaneroBonusPayoutResponseMapper implements PromoPayoutVendorResponseMapper<TransferVo> {

    @Override
    public TransferVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        TransferVo responseVo = new TransferVo();
        responseVo.setResponseCode(ResponseCodes.TRANSFER_SUCCESS);
        responseVo.getFundTransferResponseVo().setBalance(balanceData.getBalance().setScale(2, RoundingMode.DOWN));
        responseVo.getFundTransferResponseVo().setCurrencyCode(context.getVendorCurrency());
        return responseVo;
    }
}
