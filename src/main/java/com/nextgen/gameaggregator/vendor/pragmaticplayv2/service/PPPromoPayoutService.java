package com.nextgen.gameaggregator.vendor.pragmaticplayv2.service;

import com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.bet.BetVo;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.result.ResultVo;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.vo.ResponseVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PPPromoPayoutService {

    public boolean isPromoTransaction(String bonusCode) {
        return bonusCode != null;
    }

    public ResponseVo getDefaultResponseForBet(String traceId, String vendorCurrencyCode) {
        BetVo responseVo = new BetVo();

        String transactionId = VendorService.getTransactionId(traceId);
        responseVo.setTransactionId(transactionId);
        responseVo.setCurrency(vendorCurrencyCode);
        responseVo.setCash(BigDecimal.ZERO);
        responseVo.setBonus(BigDecimal.ZERO);
        responseVo.setUsedPromo(BigDecimal.ZERO);

        return responseVo;
    }

    public ResponseVo getDefaultResponseForResult(String traceId, String vendorCurrencyCode) {
        ResultVo responseVo = new ResultVo();

        String transactionId = VendorService.getTransactionId(traceId);
        responseVo.setTransactionId(transactionId);
        responseVo.setCurrency(vendorCurrencyCode);
        responseVo.setCash(BigDecimal.ZERO);
        responseVo.setBonus(BigDecimal.ZERO);

        return responseVo;
    }
}
