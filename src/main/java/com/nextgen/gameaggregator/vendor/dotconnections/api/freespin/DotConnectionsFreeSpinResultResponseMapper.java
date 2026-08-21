package com.nextgen.gameaggregator.vendor.dotconnections.api.freespin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.dotconnections.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
public class DotConnectionsFreeSpinResultResponseMapper implements PromoPayoutVendorResponseMapper<ResponseVo> {

    @Override
    public ResponseVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        ResponseDataVo responseDataVo = new ResponseDataVo();
        responseDataVo.setBrandUid(balanceData.getUsername());
        responseDataVo.setCurrency(balanceData.getCurrency());
        responseDataVo.setBalance(balanceData.getBalance());

        ResponseVo responseVo = new ResponseVo();
        responseVo.setCode(ResponseCodes.SUCCESS);
        responseVo.setData(responseDataVo);

        return responseVo;
    }
}
