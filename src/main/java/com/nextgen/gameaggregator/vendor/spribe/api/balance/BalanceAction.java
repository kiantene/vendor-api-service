package com.nextgen.gameaggregator.vendor.spribe.api.balance;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.vo.DataVo;
import com.nextgen.gameaggregator.vendor.spribe.vo.ErrorVo;
import com.nextgen.gameaggregator.vendor.spribe.vo.ResponseVo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH)
public class BalanceAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private AgentPlayerService agentPlayerService;

    @PostMapping(path = Endpoints.INFO)
    public ResponseVo authenticate(HttpServletRequest request) {
        
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        DataVo data = new DataVo();
        ErrorVo error = new ErrorVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BalanceDto dto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUser_id());
            
            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Set response data
            data.setUser_id(gameSession.getAgentPlayerId().toString());
            data.setUsername(gameSession.getAgentPlayerUsername());
            data.setBalance(balance);
            data.setCurrency(gameSession.getVendorCurrencyCode());
            vo.setData(data);

        } catch (AuthenticationException authenticationException) {
            error.setErrorCode(ErrorCodes.INVALID_TOKEN);
            vo.setError(error);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (Exception exception) {
            error.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            vo.setError(error);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, vo);
        }
        
        return vo;
    }

     private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, 
        DisabledGameException, CredentialNotFoundException {
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUser_id(), AuthenticationException::new);
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
