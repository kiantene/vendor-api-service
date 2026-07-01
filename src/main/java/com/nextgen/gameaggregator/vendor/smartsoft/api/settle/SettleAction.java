package com.nextgen.gameaggregator.vendor.smartsoft.api.settle;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Headers;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.smartsoft.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping(path = EndPoints.PATH)
public class SettleAction {
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public SettleAction(WalletService walletService,
                        HttpService httpService,
                        VendorService vendorService,
                        VendorLineService vendorLineService,
                        RequestIdempotentLogService requestIdempotentLogService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    @PostMapping(path = EndPoints.WITHDRAW)
    public ResponseEntity<SettleVo> withdrawTransaction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        String method = httpRequestLog.getMethod();
        BigDecimal balance;
        //Add request header log
        httpRequestLog.setRequestBody("Request Body: \n" + httpRequestLog.getRequestBody() + "\n\nRequest Header: \n" + vendorService.getHeaders(request));
        SettleVo vo = new SettleVo();
        SettleDto settleDto = new SettleDto();

        GameSession gameSession = null;
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.OK;
        boolean isRequestExists = false;

        try {
            settleDto = HttpService.convertJsonToDto(body, SettleDto.class);

            settleDto.setSignature(request.getHeader(Headers.REQUEST_SIGNATURE));
            settleDto.setSessionId(request.getHeader(Headers.SESSION_ID));
            settleDto.setUserName(request.getHeader(Headers.USER_NAME));
            settleDto.setClientExternalKey(request.getHeader(Headers.CLIENT_EXTERNAL_KEY));

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // Request idempotent checking.
            if (requestIdempotentLogService.checkExists(settleDto, settleDto.getUserName()) == null) {
                requestIdempotentLogService.create(settleDto, settleDto.getUserName());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // Verify session
            gameSession = vendorService.checkGameSession(traceId, settleDto.getUserName(), settleDto.getTransactionInfoDto().getGameName());

            // GA-14424: NO pre-settle balance call. The operator balance endpoint erroring here used to be
            // caught by the generic handler and returned HTTP 500 + empty {} to the vendor, which made
            // Smartsoft resend. The balance is only needed as a fallback on an idempotent resend, so it is
            // fetched lazily there (see resolveResendBalance) — never on the normal settle path.

            // Verify parameters (Verify against database values)
            this.doVerification(settleDto, gameSession, body, method);

            // Settle
            ResultType updatedResultType = vendorService.calculateResultType(settleDto.getBetAmount(), settleDto.getWinAmount(), settleDto.getJackpotAmount(), false);
            balance = walletService.processBetResult(traceId, gameSession, settleDto, updatedResultType, vendorService, httpRequestLog);

            vo.setTransactionId(httpRequestLog.getId());
            vo.setBalance(balance);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setTransactionId(e.getTransactionId());
            // GA-14424: idempotent resend — prefer the stored result balance; fetch a live balance only if it's missing.
            vo.setBalance(resolveResendBalance(e, traceId, gameSession, httpRequestLog));
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            headers.add(Headers.ERROR_CODE, ResponseCode.LOSS_LIMIT.code.toString());
            headers.add(Headers.ERROR_MESSAGE, ResponseCode.LOSS_LIMIT.message);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            headers.add(Headers.ERROR_CODE, ResponseCode.INTERNAL_ERROR.code.toString());
            headers.add(Headers.ERROR_MESSAGE, ResponseCode.INTERNAL_ERROR.message);
        } finally {
            if (!isRequestExists) {
                requestIdempotentLogService.delete(settleDto, settleDto.getUserName());
            }
            httpService.end(httpRequestLog, vo);
        }
        return new ResponseEntity<>(vo, headers, status);
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleDto dto, GameSession gameSession, String body, String method) throws AuthenticationException, CredentialNotFoundException, InvalidRequestException {

        //Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserName(), InvalidRequestException::new);

        //Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.signatureGenerator(secretKey, method, body), dto.getSignature(), AuthenticationException::new);

        //verify ClientExternalKey
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getClientExternalKey(), AuthenticationException::new);
    }

    /**
     * GA-14424: balance to return on an idempotent resend. Prefer the stored result balance carried by the
     * exception (the balance as of that settle); only when it is absent do we read the live balance — and a
     * failure of that read must never fail the response (it is logged and we return an empty balance).
     */
    private BigDecimal resolveResendBalance(BetResultIdempotentViolationException e, String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        if (e.getBalance() != null) {
            return e.getBalance();
        }
        if (gameSession == null) {
            return null;
        }
        try {
            return getCurrentBalance(traceId, gameSession, httpRequestLog);
        } catch (InvalidOperatorResponseException | InvalidAgentApiCredentialException | VendorCurrencyNotSupportException balanceError) {
            log.warn("GA-14424: Smartsoft resend balance read failed; returning empty balance. traceId={}", traceId, balanceError);
            return null;
        }
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, final HttpRequestLog httpRequestLog) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, InvalidOperatorResponseException {
        HttpRequestLog httpRequestLogdup = new HttpRequestLog(httpRequestLog);

        // Call the service with the duplicate log
        return walletService.getBalance(traceId, gameSession, httpRequestLogdup);
    }
}
