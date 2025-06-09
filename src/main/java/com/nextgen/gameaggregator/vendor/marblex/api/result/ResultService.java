package com.nextgen.gameaggregator.vendor.marblex.api.result;

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
public class ResultService {
    private static final String RESULT_ACTION = "result";
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;


    public ResultService(HttpService httpService,
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

    public CommonVo settleBet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        ResultDto resultDto = new ResultDto();
        GameSession gameSession = new GameSession();
        VendorService.IdempotentState idempotentState = null;

        try {
            resultDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ResultDto.class);
            ValidationUtils.validateRequest(resultDto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(resultDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(resultDto.getGameCode(), gameSession);

            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);
            vendorService.doDataMapper(walletRequest, resultDto);
            vendorService.doVerification(resultDto, gameSession, false);
            // Handle idempotent request check using VendorService
            idempotentState = vendorService.checkIdempotentRequest(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), RESULT_ACTION);

            // Process the settlement
            walletRequest = sportWalletService.settle(walletRequest);

            // Create idempotent log if needed (for new requests)
            vendorService.createIdempotentLogIfNeeded(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), walletRequest, idempotentState, RESULT_ACTION);

            // Recreate existing log with OK status if settlement was successful
            vendorService.recreateIdempotentLogWithOkStatus(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), walletRequest, idempotentState, RESULT_ACTION);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), walletRequest.getBalanceAfter());

        } catch (AuthenticationException | InvalidPlayerException | GameNotSupportedException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_AUTHENTICATION);
            httpService.logError(httpRequestLog, exception);

        } catch (InvalidRequestException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, exception);

        } catch (BetNotFoundException exception) {
            commonVo.setStatusCode(StatusCode.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, exception);

        } catch (BetResultIdempotentViolationException exception) {
            commonVo = vendorService.mapIdempotentSuccess(exception.getBalance(), gameSession, httpRequestLog);
            httpService.logError(httpRequestLog, exception);

        } catch (TransactionStillProcessingException exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);

        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (idempotentState != null) {
                vendorService.cleanupIdempotentLog(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), idempotentState, RESULT_ACTION);
            }

            commonVo.setTraceId(resultDto.getTraceId());
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }
}
