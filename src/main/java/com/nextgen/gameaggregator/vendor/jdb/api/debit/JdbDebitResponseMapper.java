package com.nextgen.gameaggregator.vendor.jdb.api.debit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitContext;
import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.stereotype.Component;

/**
 * Translates a successful debit result into a JDB Action 9 wire response.
 * Error paths (insufficient balance, idempotency violation, etc.) are mapped by
 * {@link JdbDebitController}'s exception handling, not this mapper.
 */
@Component
public class JdbDebitResponseMapper implements DebitVendorResponseMapper<CommonVo> {

    @Override
    public CommonVo toVendor(DebitContext context, PlayerBalanceData balanceData) {
        CommonVo vo = new CommonVo();
        vo.setBalance(balanceData.getBalance());
        vo.setSuccessResponseCode(ResponseCode.SUCCESS);
        return vo;
    }
}
