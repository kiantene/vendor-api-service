package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceVo;
import com.nextgen.gameaggregator.service.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletRequestServiceImpl implements WalletRequestService {

    private final HttpService httpService;
    private final CurrencyService currencyService;
    private final VendorPlayerService vendorPlayerService;
    private final AgentPlayerService agentPlayerService;
    private final AgentApiCredentialService agentApiCredentialService;
    private final VendorGameService vendorGameService;


    public WalletRequestServiceImpl(HttpService httpService,
                                    CurrencyService currencyService,
                                    VendorPlayerService vendorPlayerService,
                                    AgentPlayerService agentPlayerService,
                                    AgentApiCredentialService agentApiCredentialService,
                                    VendorGameService vendorGameService) {

        this.httpService = httpService;
        this.currencyService = currencyService;
        this.vendorPlayerService = vendorPlayerService;
        this.agentPlayerService = agentPlayerService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.vendorGameService = vendorGameService;
    }

    @Override
    public WalletRequest updateByGameSession(WalletRequest walletRequest, GameSession gameSession) {
        walletRequest.setOperatorUsername(gameSession.getAgentPlayerUsername());
        walletRequest.setVendorGameId(gameSession.getVendorGameId());
        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
        walletRequest.setVendorLineId(gameSession.getVendorLineId());
        walletRequest.setVendorId(gameSession.getVendorId());
        walletRequest.setAgentId(gameSession.getAgentId());
        walletRequest.setAgentPlayerId(gameSession.getAgentPlayerId());
        walletRequest.setGameCategoryId(gameSession.getGameCategoryId());
        walletRequest.setCurrencyId(gameSession.getCurrencyId());
        walletRequest.setCurrencyCode(gameSession.getCurrencyCode());
        walletRequest.setVendorPlayerId(gameSession.getVendorPlayerId());

        return walletRequest;
    }

    @Override
    public void updateByVendorUsername(WalletRequest walletRequest, String username) throws
            InvalidPlayerException, BetNotAllowedException {

        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(username);
        Long agentPlayerId = vendorPlayer.getAgentPlayerId();
        AgentPlayer agentPlayer;

        try {
            agentPlayer = agentPlayerService.get(agentPlayerId);
        } catch (RecordNotFoundException recordNotFoundException) {
            throw new InvalidPlayerException("agentPlayerId " + agentPlayerId + " cannot be found");
        }

        Integer agentId = agentPlayer.getAgentId();

        walletRequest.setAgentId(agentId);
        walletRequest.setAgentPlayerId(agentPlayerId);
        walletRequest.setOperatorUsername(agentPlayer.getUsername());
        walletRequest.setVendorId(vendorPlayer.getVendorId());
        walletRequest.setVendorLineId(vendorPlayer.getVendorLineId());
        walletRequest.setVendorPlayerId(vendorPlayer.getId());
        walletRequest.setVendorPlayerUsername(vendorPlayer.getUsername());

        try {
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);

            walletRequest.setOperatorEndpoint(agentApiCredential.getCallbackUrl());
            walletRequest.setApiSecret(agentApiCredential.getApiSecret());
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            throw new BetNotAllowedException("Agent Line (agentId: " + agentId + ") is disabled");
        }
    }

    @Override
    public void updateByVendorGameId(WalletRequest walletRequest, Integer vendorGameId) throws BetNotAllowedException {
        try {
            VendorGame vendorGame = vendorGameService.getByVendorGameId(vendorGameId);
            walletRequest.setVendorGameId(vendorGameId);
            walletRequest.setGameCode(vendorGame.getCode());
            walletRequest.setGameCategoryId(vendorGame.getGameCategoryId());

        } catch (GameNotSupportedException gameNotSupportedException) {
            throw new BetNotAllowedException(gameNotSupportedException.getClass().getSimpleName() + ": " + vendorGameId);
        }
    }

    @Override
    public void updateByCurrencyId(WalletRequest walletRequest, Integer currencyId) throws BetNotAllowedException {
        try {
            String currencyCode = currencyService.get(currencyId).getCode();
            walletRequest.setCurrencyId(currencyId);
            walletRequest.setCurrencyCode(currencyCode);

        } catch (InvalidCurrencyException invalidCurrencyException) {
            throw new BetNotAllowedException(invalidCurrencyException.getClass().getSimpleName() + ": " + currencyId);
        }
    }

    @Override
    public void validateOperatorResponse(WalletRequest request, WalletBalanceVo response)
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

    @Override
    public BigDecimal convertAmountToVendorRate(WalletBalanceVo walletBalanceVo, BigDecimal toVendorRate) {
        BigDecimal balance = walletBalanceVo.getData().getBalance();

        BigDecimal convertedBalance = balance.multiply(toVendorRate).stripTrailingZeros();
        return new BigDecimal(convertedBalance.toPlainString());
    }

    @Override
    public void end(WalletRequest walletRequest, HttpRequestLog log, HttpResponse responseVo) {
        HttpRequestLog newLog = new HttpRequestLog(log);
        newLog.setId(walletRequest.getTraceId());
        newLog.setAgentId(walletRequest.getAgentId());
        newLog.setVendorId(walletRequest.getVendorId());
        newLog.setOperatorUsername(walletRequest.getOperatorUsername());
        newLog.setVendorUsername(walletRequest.getVendorPlayerUsername());
        newLog.setVendorBetId(walletRequest.getVendorBetId());
        newLog.setRoundId(walletRequest.getRoundId());
        newLog.setVendorGameCode(walletRequest.getVendorGameCode());
        newLog.setGameToken(walletRequest.getToken());
        newLog.setOperatorEndPoints(walletRequest.getOperatorEndpoint());
        newLog.setOperatorData(walletRequest.getOperatorData());
        newLog.setOperatorHttpStatusCode(walletRequest.getOperatorHttpStatusCode());
        newLog.setOperatorResponseStatus(walletRequest.getOperatorResponseStatus());
        newLog.setOperatorResponse(walletRequest.getOperatorResponse());
        newLog.setOperatorStart(walletRequest.getOperatorStart());
        newLog.setOperatorEnd(walletRequest.getOperatorEnd());
        newLog.setBetStart(walletRequest.getBetStart());
        newLog.setBetEnd(walletRequest.getBetEnd());
        newLog.setErrorMessage(walletRequest.getErrorMessage());

        httpService.end(newLog, responseVo);
    }
}
