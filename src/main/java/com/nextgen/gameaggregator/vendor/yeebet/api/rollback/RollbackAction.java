package com.nextgen.gameaggregator.vendor.yeebet.api.rollback;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yeebet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.constant.RequestType;
import com.nextgen.gameaggregator.vendor.yeebet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
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
            this.doVerification(rollbackDto,gameSession,body);

            // 1 - failed to process deduct function (failed to place bet), 9 = failed to process deposit function (failed to settle transaction)
            if(rollbackDto.getType().equals(RequestType.BET_REQUEST) || rollbackDto.getType().equals(RequestType.SETTLE_REQUEST)){
                // Retrieve the latest wallet balance from Operator
                balance = walletService.processRollback(traceId,rollbackDto, gameSession, vendorService, httpRequestLog);

                // set vo
                responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
                responseVo.setResult(ResponseCodes.SUCCESS_CODE);
                responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));
            }

            // 7 - failed to process cancel request at deduct function (return error msg to reject the cancel request)
            if(rollbackDto.getType().equals(RequestType.CANCEL_REQUEST)){
                // set vo
                responseVo.setDesc(ResponseCodes.REJECT_CANCEL_MSG);
                responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
            }

        } catch(BetNotFoundException |
                BetRefundIdempotentViolationException |
                BetResultIdempotentViolationException e){

            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
            responseVo.setResult(ResponseCodes.SUCCESS_CODE);
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

            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.PARAMETER_ERROR_MSG);
            responseVo.setResult(ResponseCodes.PARAMETER_ERROR_CODE);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);

            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
        }finally{
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(RollbackDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RollbackDto dto, GameSession gameSession, String queryString) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, CredentialNotFoundException, InvalidRequestException {
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

        // Verify sign value
        String secret_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_secret_key);

        String converted_body = vendorService.urlDecode(queryString);

        // Trim the string from the beginning until the first "&sign"
        int signIndex = converted_body.indexOf("&sign");
        String trimmedString = converted_body.substring(0, signIndex);

        // Convert query string into map format with ASCII order
        MultiValueMap<String, String> sortedMultiValueMap = vendorService.convertToSortedMultiValueMap(trimmedString);

        // generate sign value
        String verify_sign = vendorService.generateSign(sortedMultiValueMap,secret_key);

        ValidationUtils.isEquals(verify_sign, dto.getSign(), InvalidRequestException::new);
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