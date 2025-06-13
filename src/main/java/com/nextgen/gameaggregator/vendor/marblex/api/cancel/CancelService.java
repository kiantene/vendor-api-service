package com.nextgen.gameaggregator.vendor.marblex.api.cancel;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CancelService {
    private static final String CANCEL_ACTION = "refundAll";
    public final HttpService httpService;
    public final VendorService vendorService;
    public final GameSessionService gameSessionService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;

    public CancelService(SportWalletService sportWalletService,
                         HttpService httpService,
                         VendorService vendorService,
                         GameSessionService gameSessionService,
                         WalletRequestService walletRequestService) {
        this.sportWalletService = sportWalletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.walletRequestService = walletRequestService;
    }

    public CommonVo cancel(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        CancelDto cancelDto = new CancelDto();
        GameSession gameSession = new GameSession();
        VendorService.IdempotentState idempotentState = null;

        try {
            cancelDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CancelDto.class);
            ValidationUtils.validateRequest(cancelDto);

            // Handle idempotent request check using VendorService
            idempotentState = vendorService.checkIdempotentRequest(cancelDto.getExternalTransactionId(), cancelDto.getPlayerId(), CANCEL_ACTION);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(cancelDto.getPlayerId());
            vendorService.doVerification(cancelDto, gameSession, false);
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, cancelDto);

            // Process the refund all
            walletRequest = sportWalletService.refundAll(walletRequest);

            // Check if we need to skip cleanup
            vendorService.setSkipCleanupIfSuccess(walletRequest, idempotentState, CANCEL_ACTION);

            // Recreate existing log with OK status if settlement was successful
            vendorService.recreateIdempotentLogWithOkStatus(cancelDto.getExternalTransactionId(), cancelDto.getPlayerId(), walletRequest, idempotentState, CANCEL_ACTION);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), walletRequest.getBalanceAfter());

        } catch (AuthenticationException | InvalidPlayerException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_AUTHENTICATION);
            httpService.logError(httpRequestLog, exception);

        } catch (InvalidRequestException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, exception);

        } catch (BetResultIdempotentViolationException exception) {
            commonVo = vendorService.mapIdempotentSuccess(exception.getBalance(), gameSession, httpRequestLog);
            httpService.logError(httpRequestLog, exception);

        } catch (BetNotFoundException exception) {
            commonVo.setStatusCode(StatusCode.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, exception);

        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {

            if (idempotentState != null) {
                vendorService.cleanupIdempotentLog(cancelDto.getExternalTransactionId(), cancelDto.getPlayerId(), idempotentState, CANCEL_ACTION);
            }

            commonVo.setTraceId(cancelDto.getTraceId());
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }
}
