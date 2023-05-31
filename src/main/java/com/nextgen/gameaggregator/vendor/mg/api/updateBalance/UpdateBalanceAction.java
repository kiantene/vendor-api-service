package com.nextgen.gameaggregator.vendor.mg.api.updateBalance;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.mg.constant.Headers;
import com.nextgen.gameaggregator.vendor.mg.service.VendorService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class UpdateBalanceAction {
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
    
    @PostMapping(path = Endpoints.UPDATE_BALANCE)
    public ResponseEntity<UpdateBalanceVo> updateBalance(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get start time of request
        long startTime = System.currentTimeMillis();
        // Get the trace ID from the logging
        String traceId = httpRequestLog.getId();
        HttpStatus status = HttpStatus.OK;
        UpdateBalanceVo updateBalanceVo = new UpdateBalanceVo();
        HttpHeaders headers = new HttpHeaders();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert the request body to a UpdateBalanceDto object
            UpdateBalanceDto dto = HttpService.convertJsonToDto(body, UpdateBalanceDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            switch (dto.getTxnType()) {
                case DEBIT -> {
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);
                    updateBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
                    updateBalanceVo.setBalance(betEvent.getLastBalance());
                }
                case CREDIT -> {
                    WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                    ResultType resultType = determineResultType(dto);
                    BigDecimal balance = walletService.processBetResult(traceId, gameSession, winDataDto, resultType, vendorService, httpRequestLog);
                    updateBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
                    updateBalanceVo.setBalance(balance);
                }
                default -> {
                    status = HttpStatus.BAD_REQUEST;
                }
            }
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) { // Vendor only accept status 200, 400, 402, 404, 500
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            status = HttpStatus.BAD_REQUEST;

        } catch (JsonProcessingException| InvalidAgentApiCredentialException|
            InvalidRequestException| DisabledVendorLineException| DisabledAgentPlayerException|
            DisabledGameException badRequestException) {
            status = HttpStatus.BAD_REQUEST;

        } catch (InsufficientBalanceException insufficientBalanceException) {
            status = HttpStatus.PAYMENT_REQUIRED;

        } catch (AuthenticationException| BetNotFoundException| InvalidPlayerException playerNotFoundException) {
            status = HttpStatus.NOT_FOUND;

        } catch (CouchbaseDataIntegrityException| MergedBetDataIntegrityException| BetResultIdempotentViolationException internalErrorException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (Exception exception) { // any other exception encountered
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, updateBalanceVo);
        }

        // Calculate response time and add it to the headers
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        headers.add(Headers.RESPONSE_TIMESTAMP, String.valueOf(responseTime));
        // Add back the requestId to the response headers
        headers.add(Headers.REQUEST_ID, request.getHeader(Headers.REQUEST_ID));
        // Return ResponseEntity with UpdateBalanceDto object, headers, and HTTP status code
        return new ResponseEntity<>(updateBalanceVo, headers, status);
    }

    private void doValidation(UpdateBalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(UpdateBalanceDto dto, GameSession gameSession) throws AuthenticationException, 
        DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getPlayerId());
    }

    private ResultType determineResultType(UpdateBalanceDto dto) {
        return dto.getCompleted() ? ResultType.END : (dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE);
    }
}
