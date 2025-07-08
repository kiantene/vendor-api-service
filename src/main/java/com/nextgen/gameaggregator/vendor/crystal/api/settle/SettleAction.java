package com.nextgen.gameaggregator.vendor.crystal.api.settle;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.crystal.service.VendorService;
import com.nextgen.gameaggregator.vendor.crystal.vo.CommonDataVo;
import com.nextgen.gameaggregator.vendor.crystal.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class SettleAction {
    private final HttpService httpService;
    private final VendorService vendorService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public SettleAction(HttpService httpService,
                        GameSessionService gameSessionService,
                        VendorService vendorService,
                        WalletService walletService,
                        RequestIdempotentLogService requestIdempotentLogService) {

        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.SETTLE)
    public CommonDataVo settle(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        SettleDto settleDto = new SettleDto();
        CommonDataVo commonDataVo = new CommonDataVo();
        boolean isRequestExists = false;

        try {
            String body = httpRequestLog.getRequestBody();
            settleDto = HttpService.convertJsonToDto(body, SettleDto.class);
            GameSession gameSession;
            VendorService.doValidation(settleDto);
            try {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(settleDto.getPlayerId());
                gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(settleDto.getGameId(), gameSession);
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(settleDto.getPlayerId());
                gameSessionService.updateByVendorGameCode(gameSession, settleDto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            this.doVerification(settleDto.getGameId(), settleDto.getCurrencyCode(), gameSession);

            if (requestIdempotentLogService.checkExists(settleDto, settleDto.getPlayerId()) == null) {
                requestIdempotentLogService.create(settleDto, settleDto.getPlayerId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            ResultType resultType = vendorService.calculateResultType(settleDto.getBetAmount(), settleDto.getWinAmount(), settleDto.getJackpotAmount(), false);

            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, resultType,
                    vendorService, httpRequestLog);

            commonDataVo = vendorService.prepareVo(balance, settleDto.getExternalTransactionId());

        } catch (BetResultIdempotentViolationException e) {
            commonDataVo = vendorService.prepareVo(e.getBalance(), settleDto.getExternalTransactionId());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            this.handleException(e, commonDataVo, httpRequestLog);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(settleDto, settleDto.getPlayerId());
            }
            httpService.end(httpRequestLog, commonDataVo);
        }
        return commonDataVo;
    }

    private void doVerification(String gameId, String currency, GameSession gameSession)
            throws DisabledAgentPlayerException,
            DisabledVendorLineException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        vendorService.validate(currency, gameSession);
        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameId, GameNotSupportedException::new);

    }

    @ExceptionHandler({InvalidRequestException.class, InvalidPlayerException.class,
            AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonDataVo commonDataVo, HttpRequestLog httpRequestLog) {

        if (e instanceof InvalidRequestException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.INVALID_PARAMETERS.code,
                    ResponseCodes.INVALID_PARAMETERS.message
            ));
        } else if (e instanceof AuthenticationException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.INVALID_SIGNATURE.code,
                    ResponseCodes.INVALID_SIGNATURE.message
            ));
        } else {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.PLAYER_NOT_FOUND.code,
                    ResponseCodes.PLAYER_NOT_FOUND.message
            ));
        }
        commonDataVo.setData(null);
        httpService.logError(httpRequestLog, e);
    }
}
