package com.nextgen.gameaggregator.vendor.avatarux.api.cancel;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Headers;
import com.nextgen.gameaggregator.vendor.avatarux.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import com.nextgen.gameaggregator.vendor.avatarux.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class CancelAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final SettledBetService settledBetService;

    public CancelAction(WalletService walletService,
                        HttpService httpService,
                        VendorService vendorService,
                        VendorLineService vendorLineService,
                        GameSessionService gameSessionService,
                        SettledBetService settledBetService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.settledBetService = settledBetService;
    }

    @DeleteMapping(path = EndPoints.CANCEL)
    public CancelVo cancelAction(HttpServletRequest request) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String serverAuthorization = request.getHeader(Headers.SERVER_AUTHORIZATION);
        String authorization = request.getHeader(Headers.AUTHORIZATION);
        //Add request header log
        httpRequestLog.setRequestBody("Request Body: \n" + httpRequestLog.getRequestBody() + "\nRequest Header: \n" + vendorService.getHeaders(request));
        CancelVo cancelVo = new CancelVo();
        CancelDto cancelDto;
        BigDecimal currentBalance;
        GameSession gameSession = null;

        try {
            cancelDto = HttpService.convertJsonToDto(body, CancelDto.class);
            cancelDto.setXServerAuthorization(serverAuthorization);
            cancelDto.setAuthorization(authorization);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(cancelDto);

            // Get GameSession with username
            gameSession = gameSessionService.verifyToken(authorization.substring(7));

            // Verify parameters (Verify against database values)
            this.doVerification(cancelDto, gameSession, body);

            //Cancel Bet
            cancelBet(cancelDto, gameSession, traceId, cancelVo, httpRequestLog);

        } catch (BetNotFoundException | BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            currentBalance = getCurrentBalance(traceId, gameSession, httpRequestLog);
            cancelVo.setBalance(currentBalance);
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            cancelVo.setError(new ErrorVo());
            cancelVo.getError().setCode(ResponseCode.SERVER_UNAUTHORIZED.code);
            cancelVo.getError().setMessage(ResponseCode.SERVER_UNAUTHORIZED.description);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            cancelVo.setError(new ErrorVo());
            cancelVo.getError().setCode(ResponseCode.UNKNOWN.code);
            cancelVo.getError().setMessage(ResponseCode.UNKNOWN.description);
        } finally {
            httpService.end(httpRequestLog, cancelVo);
        }
        return cancelVo;
    }

    private void doValidation(CancelDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelDto dto, GameSession gameSession, String body) throws AuthenticationException, CredentialNotFoundException, InvalidRequestException {

        //1. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getNativeId());

        //2. Verify Authorization
        String authorizationToken = dto.getAuthorization();
        if (authorizationToken == null || !authorizationToken.startsWith("Bearer ")) {
            throw new AuthenticationException();
        }
        String token = authorizationToken.substring(7);
        ValidationUtils.isEquals(gameSession.getToken(), token, AuthenticationException::new);

        //3. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);

    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, final HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogdup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogdup);
    }

    private void cancelBet(CancelDto cancelDto, GameSession gameSession, String traceId,
                           CancelVo cancelVo, HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException, InvalidOperatorResponseException, BetNotFoundException, InvalidFormatException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), cancelDto.getRoundId());

        if (settledBetList == null || settledBetList.isEmpty()) {
            walletService.processRollback(traceId, cancelDto, gameSession, vendorService, httpRequestLog);
            cancelVo.setBalance(getCurrentBalance(traceId, gameSession, httpRequestLog));
        } else {
            throw new BetResultIdempotentViolationException();
        }
    }
}
