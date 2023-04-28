package com.nextgen.gameaggregator.vendor.mg.api.login;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class LoginAction {
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
    
    @PostMapping(path = Endpoints.LOGIN)
    public ResponseEntity<LoginVo> login(HttpServletRequest request) {
        // Start the HTTP request logging
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Get the request body and trace ID from the logging
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getTraceId();
        LoginVo loginVo = new LoginVo();
        HttpStatus ok = HttpStatus.OK;

        try {
            // Convert the request body to an LoginDto object
            LoginDto dto = HttpService.convertJsonToDto(body, LoginDto.class);
            // Validate request parameters (Non-database calls)
            this.doValidation(dto);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());
            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            BigDecimal balance = walletService.getBalance(traceId, gameSession);
            loginVo.setCurrency(gameSession.getVendorCurrencyCode());
            loginVo.setBalance(balance);
            loginVo.setExtOperatorToken(gameSession.getId());
        
        } catch (AuthenticationException e) {
            HttpStatus notFound = HttpStatus.NOT_FOUND;
            return new ResponseEntity<>(notFound);
            
        } catch (JsonProcessingException| InvalidOperatorResponseException| InvalidAgentApiCredentialException|
                InvalidRequestException| DisabledVendorLineException| DisabledAgentPlayerException|
                DisabledGameException e){
                HttpStatus badRequest = HttpStatus.BAD_REQUEST;
                return new ResponseEntity<>(badRequest);
        } 
        
        return new ResponseEntity<>(loginVo, ok);
    }

    private void doValidation(LoginDto dto) throws InvalidRequestException{
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(LoginDto dto, GameSession gameSession) throws AuthenticationException, 
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
