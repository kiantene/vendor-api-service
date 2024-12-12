package com.nextgen.gameaggregator.core;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
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
    private final VendorService vendorService;


    public WalletRequestServiceImpl(HttpService httpService,
                                    CurrencyService currencyService,
                                    VendorPlayerService vendorPlayerService,
                                    AgentPlayerService agentPlayerService,
                                    AgentApiCredentialService agentApiCredentialService,
                                    VendorGameService vendorGameService,
                                    VendorService vendorService) {

        this.httpService = httpService;
        this.currencyService = currencyService;
        this.vendorPlayerService = vendorPlayerService;
        this.agentPlayerService = agentPlayerService;
        this.agentApiCredentialService = agentApiCredentialService;
        this.vendorGameService = vendorGameService;
        this.vendorService = vendorService;
    }

    @Override
    public void initialise(WalletRequest walletRequest) throws BetNotAllowedException, InternalServerException {
        walletRequest.setBetStart(System.currentTimeMillis());

        if (walletRequest.getBetStatus() == null) {
            walletRequest.setBetStatus(BetStatus.SETTLED);
        }

        this.populateAgentLineInfo(walletRequest, walletRequest.getAgentId());

        try {
            this.populateCurrencyConversionRates(walletRequest, walletRequest.getVendorId(), walletRequest.getCurrencyId());
        } catch (VendorCurrencyNotSupportException vendorCurrencyNotSupportException) {
            throw new InternalServerException(vendorCurrencyNotSupportException.getMessage());
        }
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
        walletRequest.setGameCode(gameSession.getGameCode());

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
        walletRequest.setCurrencyId(vendorPlayer.getCurrencyId());

        this.populateAgentLineInfo(walletRequest, agentId);
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
    public void updateByVendorGameCode(WalletRequest walletRequest, String vendorGameCode, boolean checkStatus) throws BetNotAllowedException, InternalServerException {

        Integer vendorId = walletRequest.getVendorId();
        if (vendorId == null) {
            throw new InternalServerException("Vendor Id cannot be null, call WalletRequest.updateByVendorUsername first");
        }

        try {
            VendorGame vendorGame = vendorGameService.getByVendorGameCode(vendorGameCode, vendorId);
            if (vendorGame == null) {
                throw new GameNotSupportedException(vendorGameCode + "-" + vendorId + " cannot be found");
            }

            if (checkStatus && vendorGame.getStatus().equals(Status.INACTIVE.code)) {
                throw new GameNotSupportedException(vendorGameCode + "-" + vendorId + " is disabled");
            }

            walletRequest.setVendorGameId(vendorGame.getId());
            walletRequest.setGameCode(vendorGame.getCode());
            walletRequest.setVendorGameCode(vendorGameCode);
            walletRequest.setGameCategoryId(vendorGame.getGameCategoryId());

            if (vendorGame.getBetDataPreprocessing() == 1) {
                walletRequest.setIsPreProcessBet(true);
            }

        } catch (GameNotSupportedException gameNotSupportedException) {
            throw new BetNotAllowedException(gameNotSupportedException.getClass().getSimpleName() + ": " + gameNotSupportedException.getMessage());
        } catch (Exception exception) {
            throw new InternalServerException(exception.getMessage());
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
    public void populateAgentLineInfo(WalletRequest walletRequest, Integer agentId) throws BetNotAllowedException {
        try {
            AgentApiCredential agentApiCredential = agentApiCredentialService.getAgentApiCredential(agentId);
            String apiUrl = agentApiCredentialService.getAgentCallbackUrlBySeamlessType(agentApiCredential);

            walletRequest.setOperatorEndpoint(apiUrl);
            walletRequest.setApiKey(agentApiCredential.getApiKey());
            walletRequest.setApiSecret(agentApiCredential.getApiSecret());
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            throw new BetNotAllowedException("Agent Line (agentId: " + agentId + ") is disabled");
        }
    }

    @Override
    public void populateCurrencyConversionRates(WalletRequest walletRequest, Integer vendorId, Integer currencyId) throws VendorCurrencyNotSupportException {
        // To retrieve currency conversion rates
        VendorCurrency vendorCurrency = vendorService.findVendorCurrency(vendorId, currencyId);
        BigDecimal fromVendorRate = vendorCurrency.getFromVendorRate();
        BigDecimal toVendorRate = vendorCurrency.getToVendorRate();

        if (fromVendorRate != null && fromVendorRate.compareTo(BigDecimal.ZERO) > 0) {
            walletRequest.setFromVendorRate(fromVendorRate);
        }

        if (toVendorRate != null && toVendorRate.compareTo(BigDecimal.ZERO) > 0) {
            walletRequest.setToVendorRate(toVendorRate);
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
        newLog.setRequestType(walletRequest.getRequestType());
//
//        if (StringUtils.hasText(walletRequest.getErrorMessage())) {
//            newLog.setStatus(HttpService.ERROR);
//        }

        httpService.end(newLog, responseVo);
    }
}
