package com.nextgen.gameaggregator.vendor.mg.api.rollback;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.mg.constant.Headers;
import com.nextgen.gameaggregator.vendor.mg.service.VendorService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class RollbackAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    
    @PostMapping(path = Endpoints.ROLLBACK)
    public ResponseEntity<RollbackVo> rollback(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);
        // Get start time of request
        long startTime = System.currentTimeMillis();
        // Get the trace ID from the logging
        String traceId = httpRequestLog.getId();
        HttpStatus status = HttpStatus.OK;
        RollbackVo rollbackVo = new RollbackVo();
        HttpHeaders headers = new HttpHeaders();
        String vendorCurrencyCode = "";
        BigDecimal balance = null;

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert the request body to a RollbackDto object
            RollbackDto dto = HttpService.convertJsonToDto(body, RollbackDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);
            rollbackVo.setCurrency(gameSession.getVendorCurrencyCode());
            rollbackVo.setBalance(balance);

        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (DisabledVendorLineException disabledVendorLineException) {
            httpService.logError(httpRequestLog, disabledVendorLineException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                //if found the bet in settled status
                status = HttpStatus.BAD_REQUEST;

            } else {
                //if found the bet other in settled status (cancel / refund)
                balance = betResultIdempotentViolationException.getBalance();
                rollbackVo.setCurrency(vendorCurrencyCode);
                rollbackVo.setBalance(balance);
            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            httpService.logError(httpRequestLog, transactionStillProcessingException);
            status = HttpStatus.BAD_REQUEST;

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            status = HttpStatus.BAD_REQUEST;

        } catch (RecordNotFoundException recordNotFoundException) {
            httpService.logError(httpRequestLog, recordNotFoundException);
            status = HttpStatus.BAD_REQUEST;

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            status = HttpStatus.BAD_REQUEST;
        
        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            httpService.logError(httpRequestLog, betRefundIdempotentViolationException);
            status = HttpStatus.BAD_REQUEST;

        } catch (Exception exception) { // any other exception encountered
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, rollbackVo);
        }

        // Calculate response time and add it to the headers
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        headers.add(Headers.RESPONSE_TIMESTAMP, String.valueOf(responseTime));
        // Add back the requestId to the response headers
        headers.add(Headers.REQUEST_ID, request.getHeader(Headers.REQUEST_ID));
        // Return ResponseEntity with RollbackDto object, headers, and HTTP status code
        return new ResponseEntity<>(rollbackVo, headers, status);
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws AuthenticationException, 
        DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), AuthenticationException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
