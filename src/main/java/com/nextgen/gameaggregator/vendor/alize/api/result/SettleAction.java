package com.nextgen.gameaggregator.vendor.alize.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.alize.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alize.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.alize.service.VendorService;
import com.nextgen.gameaggregator.vendor.alize.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH)
@Slf4j
public class SettleAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private RequestIdempotentLogService requestIdempotentLogService;

    @PostMapping(path = Endpoints.SETTLE_BET)
    public CommonVo settle(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        String traceId = httpRequestLog.getId();
        SettleDto dto = new SettleDto();
        boolean isRequestExists = false;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, SettleDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getUsername()) == null) {
                requestIdempotentLogService.create(dto, dto.getUsername());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 4. Verify session token
            GameSession gameSession;
            try {
                gameSession = gameSessionService.verifyToken(dto.getToken()); //token check
            } catch (AuthenticationException authenticationException) { //if expired
                gameSession = gameSessionService.generateNewSessionToken(dto.getUsername()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // 5. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 6. Send bet request to Operator
            ResultType resultType = determineResultType(dto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // 7. Set response data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(balance);
            responseVo.setUsername(dto.getUsername());
            responseVo.setCurrency(dto.getCurrency());
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            httpService.logError(httpRequestLog, mergedBetDataIntegrityException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidPlayerException invalidPlayerException) {
            httpService.logError(httpRequestLog, invalidPlayerException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            httpService.logError(httpRequestLog, credentialNotFoundException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            httpService.logError(httpRequestLog, invalidSignatureException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            httpService.logError(httpRequestLog, disabledVendorLineException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUsername());
            }
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, SettleDto dto, GameSession gameSession)
            throws InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException,
            AuthenticationException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {
        
        // Verify operator ID
        ValidationUtils.isEquals(vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator"), dto.getOperatorId(), CredentialNotFoundException::new);
    }

    private ResultType determineResultType(SettleDto dto) {
        return dto.getPayout().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.END;
    }
}
