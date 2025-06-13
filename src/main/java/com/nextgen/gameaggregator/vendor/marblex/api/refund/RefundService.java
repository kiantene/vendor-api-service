package com.nextgen.gameaggregator.vendor.marblex.api.refund;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RefundService {
    private static final String REFUND_ACTION = "refund";
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    public final VendorLineService vendorLineService;
    public final AgentPlayerService agentPlayerService;
    public final VendorGameService vendorGameService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;

    public RefundService(HttpService httpService,
                         GameSessionService gameSessionService,
                         WalletService walletService,
                         VendorService vendorService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         WalletRequestService walletRequestService,
                         SportWalletService sportWalletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.walletRequestService = walletRequestService;
        this.sportWalletService = sportWalletService;
    }

    public CommonVo refund(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        RefundDto refundDto = new RefundDto();
        GameSession gameSession = new GameSession();
        VendorService.IdempotentState idempotentState = null;
        log.info("Nickson-Marblex Refund Request: " + httpRequestLog.getRequestBody());
        try {
            refundDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), RefundDto.class);
            ValidationUtils.validateRequest(refundDto);

            // Handle idempotent request check using VendorService
            idempotentState = vendorService.checkIdempotentRequest(refundDto.getExternalTransactionId(), refundDto.getPlayerId(), REFUND_ACTION);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(refundDto.getPlayerId());
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doVerification(refundDto, gameSession, false);
            vendorService.doDataMapper(walletRequest, refundDto);

            // Process the refund
            walletRequest = sportWalletService.refund(walletRequest);

            // Check if we need to skip cleanup
            vendorService.setSkipCleanupIfSuccess(walletRequest, idempotentState, REFUND_ACTION);

            // Recreate existing log with OK status if settlement was successful
            vendorService.recreateIdempotentLogWithOkStatus(refundDto.getExternalTransactionId(), refundDto.getPlayerId(), walletRequest, idempotentState, REFUND_ACTION);

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
                vendorService.cleanupIdempotentLog(refundDto.getExternalTransactionId(), refundDto.getPlayerId(), idempotentState, REFUND_ACTION);
            }

            commonVo.setTraceId(refundDto.getTraceId());
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }
}
