package com.nextgen.gameaggregator.vendor.marblex.api.refund;

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
public class RefundService {
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;

    public RefundService(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorService vendorService, WalletRequestService walletRequestService, SportWalletService sportWalletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.sportWalletService = sportWalletService;
    }

    public CommonVo refund(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        RefundDto refundDto = new RefundDto();
        GameSession gameSession = new GameSession();

        try {
            refundDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), RefundDto.class);

            ValidationUtils.validateRequest(refundDto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(refundDto.getPlayerId());

            vendorService.doVerification(refundDto, gameSession, false);

            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, refundDto);

            walletRequest = sportWalletService.refund(walletRequest);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), walletRequest.getBalanceAfter());

        } catch (AuthenticationException | InvalidPlayerException | InvalidCurrencyException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_AUTHENTICATION);
            httpService.logError(httpRequestLog, exception);
        } catch (
                InvalidRequestException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, exception);
        } catch (
                InvalidOperatorResponseException exception) {
            commonVo.setStatusCode(StatusCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);
        } catch (BetResultIdempotentViolationException exception) {
            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), exception.getBalance());
            httpService.logError(httpRequestLog, exception);
        } catch (BetNotFoundException exception) {
            commonVo.setStatusCode(StatusCode.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, exception);
        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            commonVo.setTraceId(refundDto.getTraceId());
            walletRequestService.end(walletRequest, httpRequestLog, commonVo);
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }
}
