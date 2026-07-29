package com.nextgen.gameaggregator.vendor.evoplay.api.v2.balanceIncrease;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.WalletService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private final WalletService walletService;

    public BalanceService(WalletService walletService) {
        this.walletService = walletService;
    }

    @Cacheable(value = "EvoPlayBalance", key = "#vendorPlayerUsername", cacheManager = "cacheManager")
    public BigDecimal getBalance(String vendorPlayerUsername, String traceId, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws
            InvalidOperatorResponseException,
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException {

        // Fetch the current balance from the cache (if available)

        return walletService.getBalance(traceId, gameSession, httpRequestLog);
    }

    @CachePut(value = "EvoPlayBalance", key = "#vendorPlayerUsername", cacheManager = "cacheManager")
    public BigDecimal increaseBalance(String vendorPlayerUsername, BigDecimal currentBalance, BigDecimal increaseAmount) {

        return currentBalance.add(increaseAmount);
    }
}
