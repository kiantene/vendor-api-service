package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.HttpResponse;

import java.math.BigDecimal;

public interface WalletRequestService {
    static WalletRequest init(HttpRequestLog httpRequestLog) {
        WalletRequest request = new WalletRequest(httpRequestLog.getId());

        request.setRequestBody(httpRequestLog.getRequestBody());

        return request;
    }

    static void validateOperatorResponse(WalletRequest request, WalletBalanceVo response)
            throws InvalidOperatorResponseException, InsufficientBalanceException {

        final Integer INVALID_RESPONSE = ResponseCodes.Status.SC_INVALID_RESPONSE.code;
        WalletBalanceVo.ResponseData responseData = response.getData();

        if (!response.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            if (response.getStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS)) {
                throw new InsufficientBalanceException();
            } else {
                throw new InvalidOperatorResponseException(response.getStatus().code);
            }
        }

        String username = responseData.getUsername();
        String currency = responseData.getCurrency();
        BigDecimal balance = responseData.getBalance();

        if (!request.getTraceId().equals(response.getTraceId())) {
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        }

        if (username == null || currency == null || balance == null) {
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        }

        if (!request.getOperatorUsername().equals(username) || !request.getCurrencyCode().equals(currency)) {
            throw new InvalidOperatorResponseException(INVALID_RESPONSE);
        }
    }

    void initialise(WalletRequest walletRequest) throws BetNotAllowedException, InternalServerException;

    WalletRequest updateByGameSession(WalletRequest walletRequest, GameSession gameSession);

    void updateByVendorUsername(WalletRequest walletRequest, String username) throws InvalidPlayerException, BetNotAllowedException;

    void updateByVendorGameId(WalletRequest walletRequest, Integer vendorGameId) throws BetNotAllowedException;

    void updateByVendorGameCode(WalletRequest walletRequest, String vendorGameCode, boolean checkStatus) throws BetNotAllowedException, InternalServerException;

    void updateByCurrencyId(WalletRequest walletRequest, Integer currencyId) throws BetNotAllowedException;

    void populateAgentLineInfo(WalletRequest walletRequest, Integer agentId) throws BetNotAllowedException;

    void populateCurrencyConversionRates(WalletRequest walletRequest, Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException;

    BigDecimal convertAmountToVendorRate(WalletBalanceVo walletBalanceVo, BigDecimal toVendorRate);

    void end(WalletRequest walletRequest, HttpRequestLog log, HttpResponse responseVo);
}
