package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.core.common.VendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import org.springframework.stereotype.Component;

@Component
class AuthenticateResponseMapper implements VendorResponseMapper<AuthenticateContext, CommonVo> {
    @Override
    public CommonVo toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseSuccess(balanceData.getBalance(), context.getVendorPlayerUsername(), context.getVendorPlayerUsername());
        return responseVo;
    }
}
