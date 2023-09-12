package com.nextgen.gameaggregator.vendor.spribe.api.rollback;

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
    public ResponseVo rollback(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo vo = new ResponseVo();
        DataVo data = new DataVo();
        ErrorVo error = new ErrorVo();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            RollbackDto dto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUser_id());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal oldBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 6. Send rollback request to Operator
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService);

            // 7. Set response data
            data.setOperator_tx_id(traceId);
            data.setNew_balance(balance);
            data.setOld_balance(oldBalance);
            data.setUser_id(gameSession.getVendorPlayerUsername());
            data.setCurrency(gameSession.getVendorCurrencyCode());
            data.setProvider(dto.getProvider());
            data.setProvider_tx_id(dto.getProvider_tx_id());
        
        } catch (Exception exception) {
            error.setErrorCode(ErrorCodes.INTERNAL_ERROR);
            vo.setError(error);
        }

        return vo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws AuthenticationException, 
        DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
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
