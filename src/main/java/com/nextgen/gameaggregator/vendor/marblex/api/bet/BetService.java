package com.nextgen.gameaggregator.vendor.marblex.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
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
import org.springframework.stereotype.Service;

@Service
public class BetService {
    private static final String BET_ACTION = "bet";
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;

    public BetService(HttpService httpService,
                      GameSessionService gameSessionService,
                      WalletService walletService,
                      VendorService vendorService,
                      WalletRequestService walletRequestService,
                      SportWalletService sportWalletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.sportWalletService = sportWalletService;
    }

    public CommonVo placeBet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        BetDto betDto = new BetDto();
        GameSession gameSession = new GameSession();
        VendorService.IdempotentState idempotentState = null;

        try {
            betDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), BetDto.class);
            ValidationUtils.validateRequest(betDto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getGameCode(), gameSession);
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, betDto);
            vendorService.doVerification(betDto, gameSession, true);
            //validate currency
            ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);
            // Handle idempotent request check using VendorService
            idempotentState = vendorService.checkIdempotentRequest(betDto.getExternalTransactionId(), betDto.getPlayerId(), BET_ACTION);

            // Process the bet
            walletRequest = sportWalletService.placeBet(walletRequest);

            // Create idempotent log if needed (for new requests)
            vendorService.createIdempotentLogIfNeeded(betDto.getExternalTransactionId(), betDto.getPlayerId(), walletRequest, idempotentState, BET_ACTION);

            // Recreate existing log with OK status if settlement was successful
            vendorService.recreateIdempotentLogWithOkStatus(betDto.getExternalTransactionId(), betDto.getPlayerId(), walletRequest, idempotentState, BET_ACTION);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), walletRequest.getBalanceAfter());

        } catch (AuthenticationException | InvalidPlayerException | GameNotSupportedException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_AUTHENTICATION);
            httpService.logError(httpRequestLog, exception);

        } catch (InvalidCurrencyException | CurrencyNotSupportedException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_CURRENCY);
            httpService.logError(httpRequestLog, exception);

        } catch (InsufficientBalanceException exception) {
            commonVo.setStatusCode(StatusCode.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, exception);

        } catch (InvalidRequestException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, exception);

        } catch (BetResultIdempotentViolationException exception) {
            commonVo = vendorService.mapIdempotentSuccess(exception.getBalance(), gameSession, httpRequestLog);
            httpService.logError(httpRequestLog, exception);

        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            if (idempotentState != null) {
                vendorService.cleanupIdempotentLog(betDto.getExternalTransactionId(), betDto.getPlayerId(), idempotentState, BET_ACTION);
            }

            commonVo.setTraceId(betDto.getTraceId());
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }
}
