package com.nextgen.gameaggregator.vendor.bombay.api.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bombay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bombay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bombay.service.VendorService;
import com.nextgen.gameaggregator.vendor.bombay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollbakcAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.ROLLBACK)
    public ResponseVo rollBack(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        RollbackDto rollbackDto = null;

        GameSession gameSession = new GameSession();

        try{
            String body = httpRequestLog.getRequestBody();

            rollbackDto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(rollbackDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setBalance(balance.intValue());
        } catch(BetNotFoundException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_TRANSACTION_DOES_NOT_EXIST);
        } catch(BetRefundIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_DUPLICATE_REQUEST);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally {
            responseVo.setRequest_uuid(rollbackDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
