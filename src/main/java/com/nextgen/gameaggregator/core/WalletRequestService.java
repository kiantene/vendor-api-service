package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.HttpResponse;

import java.math.BigDecimal;

public interface WalletRequestService {
    static WalletRequest init(HttpRequestLog httpRequestLog) {
        WalletRequest request = new WalletRequest(httpRequestLog.getId());

        request.setRequestBody(httpRequestLog.getRequestBody());

        return request;
    }

    WalletRequest updateByGameSession(WalletRequest walletRequest, GameSession gameSession);

    void updateByVendorUsername(WalletRequest walletRequest, String username) throws InvalidPlayerException, BetNotAllowedException;

    void updateByVendorGameId(WalletRequest walletRequest, Integer vendorGameId) throws BetNotAllowedException;

    void updateByCurrencyId(WalletRequest walletRequest, Integer currencyId) throws BetNotAllowedException;

    void validateOperatorResponse(WalletRequest request, WalletBalanceVo response) throws InvalidOperatorResponseException, InsufficientBalanceException;

    BigDecimal convertAmountToVendorRate(WalletBalanceVo walletBalanceVo, BigDecimal toVendorRate);

    void end(WalletRequest walletRequest, HttpRequestLog log, HttpResponse responseVo);
}
