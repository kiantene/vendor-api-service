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
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;
    
    @PostMapping(path = Endpoints.UPDATE_BALANCE)
    public ResponseEntity<UpdateBalanceVo> updateBalance(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get start time of request
        long startTime = System.currentTimeMillis();
        // Get the request body and trace ID from the logging
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getId();
        HttpStatus status;
        UpdateBalanceVo updateBalanceVo = new UpdateBalanceVo();
        HttpHeaders headers = new HttpHeaders();
        status = HttpStatus.OK;

        try {
            // Convert the request body to a UpdateBalanceDto object
            UpdateBalanceDto dto = HttpService.convertJsonToDto(body, UpdateBalanceDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            status = HttpStatus.OK;
            
            switch (dto.getTxnType()) {
                case DEBIT -> {
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);
                    updateBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
                    updateBalanceVo.setBalance(betEvent.getLastBalance());
                    break;
                }
                case CREDIT -> {
                    WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                    if (dto.getCompleted() == false) {
                        walletService.processBetResult(traceId, gameSession, winDataDto,
                            (dto.getAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE, vendorService, body);
                    } else {
                        walletService.processBetResult(traceId, gameSession, winDataDto, ResultType.END, vendorService, body);
                    }
                    break;
                }
                default -> {
                    status = HttpStatus.BAD_REQUEST;
                }
            }
            
        } catch (JsonProcessingException| InvalidOperatorResponseException| InvalidAgentApiCredentialException|
            InvalidRequestException| DisabledVendorLineException| DisabledAgentPlayerException|
            DisabledGameException e){
            status = HttpStatus.BAD_REQUEST;
        } catch (InsufficientBalanceException e) {
            status = HttpStatus.PAYMENT_REQUIRED;
        } catch (AuthenticationException| BetNotFoundException e) {
            status = HttpStatus.NOT_FOUND;
        } catch (CouchbaseDataIntegrityException| MergedBetDataIntegrityException| BetResultIdempotentViolationException e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
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

    private void doValidation(UpdateBalanceDto dto) throws InvalidRequestException{
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(UpdateBalanceDto dto, GameSession gameSession) throws AuthenticationException, 
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
