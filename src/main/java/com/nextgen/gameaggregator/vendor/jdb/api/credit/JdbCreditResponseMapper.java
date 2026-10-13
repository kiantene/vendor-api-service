package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditContext;
import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.stereotype.Component;

/**
 * Shared success-response mapper for JDB credit flows — both Action 10 (settle)
 * and Action 11 (cancel) produce identical {@link CommonVo} shape on success.
 * Used by both {@code JdbSettleController} and {@code JdbRefundController}.
 */
@Component
public class JdbCreditResponseMapper implements CreditVendorResponseMapper<CommonVo> {

    @Override
    public CommonVo toVendor(CreditContext context, PlayerBalanceData balanceData) {
        CommonVo vo = new CommonVo();
        vo.setBalance(balanceData.getBalance());
        vo.setSuccessResponseCode(ResponseCode.SUCCESS);
        return vo;
    }
}
