package com.nextgen.gameaggregator.vendor.evoplay.api.balanceIncrease;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.VendorCurrencyNotSupportException;
import com.nextgen.gameaggregator.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {

    @Autowired
    private WalletService walletService;

    @Cacheable(value = "EvoPlayBalance", key = "#vendorPlayerUsername", cacheManager = "cacheManager")
    public BigDecimal getBalance(String vendorPlayerUsername, String traceId, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws
            InvalidOperatorResponseException,
            InvalidAgentApiCredentialException,
            VendorCurrencyNotSupportException {

        // Fetch the current balance from the cache (if available)
        BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        return balance;
    }

    @CachePut(value = "EvoPlayBalance", key = "#vendorPlayerUsername", cacheManager = "cacheManager")
    public BigDecimal increaseBalance(String vendorPlayerUsername, BigDecimal currentBalance, BigDecimal increaseAmount) {

        BigDecimal balance = currentBalance.add(increaseAmount);

        return balance;
    }
}
