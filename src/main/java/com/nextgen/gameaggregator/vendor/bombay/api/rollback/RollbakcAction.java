package com.nextgen.gameaggregator.vendor.bombay.api.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bombay.constant.Credentials;
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
import java.util.Map;

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

        BigDecimal balance = null;

        try{
            String body = httpRequestLog.getRequestBody();

            rollbackDto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // get x-signature value for validation
            Map<String,String> header = vendorService.headersToHashMap(request);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(rollbackDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto, gameSession, header.get("x-signature"), body);

            // Retrieve the latest wallet balance from Operator
            balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setBalance(balance.intValue());
        } catch(BetNotFoundException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_TRANSACTION_DOES_NOT_EXIST);
        } catch(BetRefundIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);

            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setCurrency(gameSession.getCurrencyCode());
            responseVo.setBalance(balance.intValue());
        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_WRONG_SYNTAX);
        } catch(InvalidSignatureException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_SIGNATURE);
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

    private void doVerification(RollbackDto dto, GameSession gameSession, String x_signature, String request_body) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException, CredentialNotFoundException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify vendor's x-signature
        String convertedJsonString = vendorService.convertObjectMapper(request_body); // Convert json beautified format back to compact JSON string

        String vendor_public_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.vendor_public_key);
        Boolean validateSignature = vendorService.validateSignature(x_signature, convertedJsonString, vendor_public_key);

        // validateSignature not equal to true mean credential problem or this data is not from vendor
        if(!validateSignature){
            throw new InvalidSignatureException();
        }
    }

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {

        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        } catch (Exception exception) {

        }

        return balance;
    }
}
