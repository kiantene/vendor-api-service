package com.nextgen.gameaggregator.vendor.marblex.api.result;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
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
    private final RequestIdempotentLogService requestIdempotentLogService;

    public ResultService(HttpService httpService,
                         GameSessionService gameSessionService,
                         WalletService walletService,
                         VendorService vendorService,
                         WalletRequestService walletRequestService,
                         SportWalletService sportWalletService,
                         RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.sportWalletService = sportWalletService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo settleBet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        ResultDto resultDto = new ResultDto();
        GameSession gameSession = new GameSession();
        boolean isRequestExists = false;

        try {
            resultDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ResultDto.class);
            ValidationUtils.validateRequest(resultDto);

            if (requestIdempotentLogService.getSportsRequestIdempotentLog(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), RESULT_ACTION) == null) {
                requestIdempotentLogService.create(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), RESULT_ACTION);
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Temporary solution: Check settled bet status using KV search.
            // TODO: Replace with a more robust mechanism in the future. (Discussed with Jeff, 18 Jun 2025)
            vendorService.checkSettledBetStatus(resultDto.getPlayerId(), resultDto.getVendorBetId());

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(resultDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(resultDto.getGameCode(), gameSession);
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, resultDto);
            vendorService.doVerification(resultDto, gameSession, false);

            // Process the settlement
            walletRequest = sportWalletService.settle(walletRequest);

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

        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(resultDto.getExternalTransactionId(), resultDto.getPlayerId(), RESULT_ACTION);
            }

            commonVo.setTraceId(resultDto.getTraceId());
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }
}
