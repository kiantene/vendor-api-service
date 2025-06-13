package com.nextgen.gameaggregator.vendor.marblex.api.refund;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
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
    private static final String LOG_ACTION = "log_refund";

    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    public final VendorLineService vendorLineService;
    public final AgentPlayerService agentPlayerService;
    public final VendorGameService vendorGameService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public RefundService(HttpService httpService,
                         GameSessionService gameSessionService,
                         WalletService walletService,
                         VendorService vendorService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         WalletRequestService walletRequestService,
                         SportWalletService sportWalletService, RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.walletRequestService = walletRequestService;
        this.sportWalletService = sportWalletService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo refund(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        RefundDto refundDto = new RefundDto();
        GameSession gameSession = new GameSession();
        VendorService.IdempotentState idempotentState = null;
        boolean isRequestExists = false;
        log.info("Nickson-Marblex Refund Request: " + httpRequestLog.getRequestBody());
        try {
            refundDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), RefundDto.class);
            ValidationUtils.validateRequest(refundDto);

            if (requestIdempotentLogService.getSportsRequestIdempotentLog(refundDto.getExternalTransactionId(),
                    refundDto.getVendorPlayerUsername(),
                    LOG_ACTION) == null) {
                requestIdempotentLogService.create(refundDto.getExternalTransactionId(),
                        refundDto.getVendorPlayerUsername(),
                        walletRequest.getOperatorResponseStatus().code,
                        LOG_ACTION);
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(refundDto.getPlayerId());
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doVerification(refundDto, gameSession, false);
            vendorService.doDataMapper(walletRequest, refundDto);

            // Handle idempotent request check using VendorService
            idempotentState = vendorService.checkIdempotentRequest(refundDto.getExternalTransactionId(), refundDto.getPlayerId(), REFUND_ACTION);

            // Process the refund
            walletRequest = sportWalletService.refund(walletRequest);

            // Create idempotent log if needed (for new requests)
            vendorService.createIdempotentLogIfNeeded(refundDto.getExternalTransactionId(), refundDto.getPlayerId(), walletRequest, idempotentState, REFUND_ACTION);

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

            if (!isRequestExists) {
                requestIdempotentLogService.delete(refundDto.getExternalTransactionId(),
                        refundDto.getVendorPlayerUsername(),
                        LOG_ACTION);
            }

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
