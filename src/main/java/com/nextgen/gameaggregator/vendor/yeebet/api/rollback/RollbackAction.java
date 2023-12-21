package com.nextgen.gameaggregator.vendor.yeebet.api.rollback;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yeebet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class RollbackAction {
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
    VendorService vendorService;

    @PostMapping(path = EndPoints.ROLLBACK)
    public ResponseVo rollBack(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        GameSession gameSession = new GameSession();

        BigDecimal balance = null;

        ResponseVo responseVo = new ResponseVo();

        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            RollbackDto rollbackDto = httpService.convertQueryStringToDtoUrlDecode(body, RollbackDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(rollbackDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(rollbackDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto,gameSession);

            // 1 - failed to process deduct function (failed to place bet), 9 = failed to process deposit function (failed to settle transaction)
            if(rollbackDto.getType().equals("1") || rollbackDto.getType().equals("9")){
                // Retrieve the latest wallet balance from Operator
                balance = walletService.processRollback(traceId,rollbackDto, gameSession, vendorService, httpRequestLog);

                // set vo
                responseVo.setResult(0);
                responseVo.setDesc("Success");
                responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));
            }

            // 7 - failed to process cancel request at deduct function (return error msg to reject the cancel request)
            if(rollbackDto.getType().equals("7")){
                // set vo
                responseVo.setDesc("Refuse to cancel this transaction");
                responseVo.setResult(-1000);
            }

        } catch(BetNotFoundException |
                BetRefundIdempotentViolationException |
                BetResultIdempotentViolationException e){

            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setResult(0);
            responseVo.setDesc("Success");
            responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));

        } catch(VendorCurrencyNotSupportException |
                AuthenticationException |
                InvalidOperatorResponseException |
                DisabledVendorLineException |
                InvalidAgentApiCredentialException |
                InvalidPlayerException |
                DisabledAgentPlayerException |
                DisabledGameException |
                RecordNotFoundException |
                TransactionStillProcessingException e){
            httpService.logError(httpRequestLog, e);

            responseVo.setDesc("The system is error, please contact");
            responseVo.setResult(-1000);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc("Parameter error,please check");
            responseVo.setResult(-1002);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);

            responseVo.setDesc("The system is error, please contact");
            responseVo.setResult(-1000);
        }finally{
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, CredentialNotFoundException, InvalidRequestException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        //Verify received appid is same with credential
        String appid = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_app_id);
        ValidationUtils.isEquals(appid, dto.getAppid(), InvalidRequestException::new);
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