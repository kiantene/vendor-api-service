package com.nextgen.gameaggregator.vendor.pragmaticplayv2.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.bet.BetVo;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.result.ResultVo;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.vo.ResponseVo;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PPPromoPayoutService {
    private static final String PROVIDER_NAME = "PragmaticPlay";
    private final WalletService walletService;
    private final MeterRegistry meterRegistry;

    public PPPromoPayoutService(WalletService walletService, MeterRegistry meterRegistry) {
        this.walletService = walletService;
        this.meterRegistry = meterRegistry;
    }

    public boolean isPromoTransaction(String bonusCode) {
        return bonusCode != null;
    }

    public ResponseVo getDefaultResponseForBet(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog, String vendorCurrencyCode) {

        meterRegistry.counter("pp.freeround.bet.count",
                        "provider", PROVIDER_NAME,
                        "vendorId", String.valueOf(gameSession.getVendorId()),
                        "vendorLineId", String.valueOf(gameSession.getVendorLineId()))
                .increment();
        BetVo responseVo = new BetVo();
        BigDecimal balance = BigDecimal.ZERO;
        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        } catch (Exception exception) {

        }
        String transactionId = VendorService.getTransactionId(traceId);
        responseVo.setTransactionId(transactionId);
        responseVo.setCurrency(vendorCurrencyCode);
        responseVo.setCash(balance);
        responseVo.setBonus(BigDecimal.ZERO);
        responseVo.setUsedPromo(BigDecimal.ZERO);

        return responseVo;
    }

    public ResponseVo getDefaultResponseForResult(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog, String vendorCurrencyCode) {
        ResultVo responseVo = new ResultVo();
        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
        } catch (Exception exception) {
            // Optional: log this, but keep default response behavior
        }

        String transactionId = VendorService.getTransactionId(traceId);
        responseVo.setTransactionId(transactionId);
        responseVo.setCurrency(vendorCurrencyCode);
        responseVo.setCash(balance);
        responseVo.setBonus(BigDecimal.ZERO);

        return responseVo;
    }
}
