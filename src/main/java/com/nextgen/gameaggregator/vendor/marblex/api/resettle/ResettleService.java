package com.nextgen.gameaggregator.vendor.marblex.api.resettle;

import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ResettleService {
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    private final SportWalletService sportWalletService;
    private static final String RESETTLE_ACTION = "resettle";

    public ResettleService(HttpService httpService, 
                           GameSessionService gameSessionService, 
                           WalletService walletService,
                           VendorService vendorService, 
                           SportWalletService sportWalletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.sportWalletService = sportWalletService;
    }

    public CommonVo resettle(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        CommonVo commonVo = new CommonVo();
        ResettleDto resettleDto = new ResettleDto();
        GameSession gameSession = new GameSession();
        VendorService.IdempotentState idempotentState = null;

        try {
            resettleDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ResettleDto.class);
            ValidationUtils.validateRequest(resettleDto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(resettleDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(resettleDto.getGameCode(), gameSession);
            vendorService.doVerification(resettleDto, gameSession, false);

            // Handle idempotent request check using VendorService
            idempotentState = vendorService.checkIdempotentRequest(resettleDto.getExternalTransactionId(), resettleDto.getPlayerId(), RESETTLE_ACTION);

            // Process the adjustment
            BetEvent betEvent = sportWalletService.adjustment(resettleDto.getTraceId(), resettleDto, httpRequestLog);

            // Create idempotent log if needed (for new requests)
            //vendorService.createIdempotentLogIfNeeded(resettleDto.getExternalTransactionId(), resettleDto.getPlayerId(), walletRequest, idempotentState, RESETTLE_ACTION);
            
            // Recreate existing log with OK status if settlement was successful
            // vendorService.recreateIdempotentLogWithOkStatus(resettleDto.getExternalTransactionId(), resettleDto.getPlayerId(), walletRequest, idempotentState, RESETTLE_ACTION);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), betEvent.getLastBalance());

        } catch (AuthenticationException | InvalidPlayerException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_AUTHENTICATION);
            httpService.logError(httpRequestLog, exception);

        } catch (InsufficientBalanceException exception) {
            commonVo.setStatusCode(StatusCode.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, exception);

        } catch (BetNotFoundException exception) {
            commonVo.setStatusCode(StatusCode.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, exception);

        } catch (InvalidRequestException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, exception);

        } catch (BetAdjustmentIdempotentViolationException exception) {
            commonVo = vendorService.mapIdempotentSuccess(exception.getRawBetAdjustmentLog().getBalance(), gameSession, httpRequestLog);
            httpService.logError(httpRequestLog, exception);

        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            if (idempotentState != null) {
                vendorService.cleanupIdempotentLog(resettleDto.getExternalTransactionId(), resettleDto.getPlayerId(), idempotentState, RESETTLE_ACTION);
            }

            commonVo.setTraceId(resettleDto.getTraceId());
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }
}
