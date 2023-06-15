package com.nextgen.gameaggregator.vendor.mg.api.balance;

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
public class GetBalanceAction {
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
    
    @PostMapping(path = Endpoints.GET_BALANCE)
    public ResponseEntity<GetBalanceVo> getBalance(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get start time of request
        long startTime = System.currentTimeMillis();
        // Get the trace ID from the logging
        String traceId = httpRequestLog.getId();
        HttpStatus status = HttpStatus.OK;
        GetBalanceVo getBalanceVo = new GetBalanceVo();
        HttpHeaders headers = new HttpHeaders();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            // Convert the request body to a GetBalanceDto object
            GetBalanceDto dto = HttpService.convertJsonToDto(body, GetBalanceDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            // Get Player balance from Wallet Service
            BigDecimal balance = walletService.getBalance(traceId, gameSession);
            getBalanceVo.setCurrency(gameSession.getVendorCurrencyCode());
            getBalanceVo.setBalance(balance);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) { // Vendor only accept status 200 and 401
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            status = HttpStatus.UNAUTHORIZED;

        } catch (JsonProcessingException| InvalidAgentApiCredentialException|
            InvalidRequestException| DisabledVendorLineException| DisabledAgentPlayerException|
            DisabledGameException| AuthenticationException invalidException) {
            status = HttpStatus.UNAUTHORIZED;

        } catch (Exception exception) { // any other exception encountered
            status = HttpStatus.UNAUTHORIZED;
            httpService.logError(httpRequestLog, exception);
            
        } finally {
            httpService.end(httpRequestLog, getBalanceVo);
        }

        // Calculate response time and add it to the headers
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        headers.add(Headers.RESPONSE_TIMESTAMP, String.valueOf(responseTime));
        // Add back the requestId to the response headers
        headers.add(Headers.REQUEST_ID, request.getHeader(Headers.REQUEST_ID));
        // Return ResponseEntity with GetBalanceVo object, headers, and HTTP status code
        return new ResponseEntity<>(getBalanceVo, headers, status);
    }

    private void doValidation(GetBalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GetBalanceDto dto, GameSession gameSession) throws AuthenticationException, 
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
