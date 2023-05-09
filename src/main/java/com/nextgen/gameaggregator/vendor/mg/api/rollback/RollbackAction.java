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
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.mg.constant.Headers;

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
    
    @PostMapping(path = Endpoints.ROLLBACK)
    public ResponseEntity<RollbackVo> updateBalance(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);
        // Get start time of request
        long startTime = System.currentTimeMillis();
        // Get the request body and trace ID from the logging
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getTraceId();
        HttpStatus status = HttpStatus.OK;
        RollbackVo rollbackVo = new RollbackVo();
        HttpHeaders headers = new HttpHeaders();

        try {
            // Convert the request body to a UpdateBalanceDto object
            RollbackDto dto = HttpService.convertJsonToDto(body, RollbackDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession);
            rollbackVo.setExtTxnId(dto.getTxnId());
            rollbackVo.setCurrency(gameSession.getVendorCurrencyCode());
            rollbackVo.setBalance(balance);
            rollbackVo.setExtCreationTimeMs(startTime);
        } catch (BetRefundIdempotentViolationException| JsonProcessingException| InvalidOperatorResponseException| InvalidAgentApiCredentialException|
            InvalidRequestException| DisabledVendorLineException| DisabledAgentPlayerException|
            DisabledGameException| AuthenticationException| RecordNotFoundException e){
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } finally {
            httpService.end(httpRequestLog, rollbackVo);
        }

        // Calculate response time and add it to the headers
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        headers.add(Headers.RESPONSE_TIMESTAMP, String.valueOf(responseTime));
        // Add back the requestId to the response headers
        headers.add(Headers.REQUEST_ID, request.getHeader(Headers.REQUEST_ID));
        // Return ResponseEntity with UpdateBalanceDto object, headers, and HTTP status code
        return new ResponseEntity<>(rollbackVo, headers, status);
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException{
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws AuthenticationException, 
        DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException{
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
