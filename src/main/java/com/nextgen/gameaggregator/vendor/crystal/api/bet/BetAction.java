package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BetAction {
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public BetAction(HttpService httpService,
                     ValidationService validationService,
                     WalletService walletService,
                     GameSessionService gameSessionService,
                     VendorService vendorService,
                     RequestIdempotentLogService requestIdempotentLogService) {
        this.validationService = validationService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.BET)
    public CommonDataVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonDataVo commonDataVo = new CommonDataVo();
        BetDto betDto = new BetDto();
        boolean isRequestExists = false;
        try {
            String body = httpRequestLog.getRequestBody();
            betDto = HttpService.convertJsonToDto(body, BetDto.class);

            VendorService.doValidation(betDto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getPlayerId());

            vendorService.doCompareSignature(request, httpRequestLog, gameSession);
            this.doVerification(betDto.getGameId(), betDto.getCurrencyCode(), gameSession);

            if (requestIdempotentLogService.checkExists(betDto, betDto.getPlayerId()) == null) {
                requestIdempotentLogService.create(betDto, betDto.getPlayerId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }
            Thread.sleep(5000);
            // Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto,
                    httpRequestLog.getRequestBody(), httpRequestLog);

            //Set response data
            commonDataVo = vendorService.prepareVo(betEvent.getLastBalance(), betDto.getExternalTransactionId());

        } catch (BetResultIdempotentViolationException e) {
            commonDataVo = vendorService.prepareVo(e.getBalance(), betDto.getExternalTransactionId());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            this.handleException(e, commonDataVo, httpRequestLog);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(betDto, betDto.getPlayerId());
            }
            httpService.end(httpRequestLog, commonDataVo);
        }
        return commonDataVo;
    }

    private void doVerification(String gameId, String currency, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        //check session gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameId, GameNotSupportedException::new);
        vendorService.validate(currency, gameSession);
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