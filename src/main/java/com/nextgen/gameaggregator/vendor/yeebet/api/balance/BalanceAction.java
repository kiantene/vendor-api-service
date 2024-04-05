package com.nextgen.gameaggregator.vendor.yeebet.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yeebet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class BalanceAction {
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

    @GetMapping (path = EndPoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        httpRequestLog.setRequestBody(request.getQueryString());

        String traceId = httpRequestLog.getId();

        GameSession gameSession = new GameSession();

        ResponseVo responseVo = new ResponseVo();

        try{
            String body = httpRequestLog.getRequestBody();

            BalanceDto balanceDto = httpService.convertQueryStringToDto(body,BalanceDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(balanceDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession, body);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
            responseVo.setResult(ResponseCodes.SUCCESS_CODE);
            responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));

        } catch(InvalidAgentApiCredentialException |
                InvalidPlayerException |
                VendorCurrencyNotSupportException |
                AuthenticationException |
                DisabledAgentPlayerException |
                DisabledGameException |
                InvalidRequestException |
                InvalidOperatorResponseException |
                DisabledVendorLineException e){

            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);

        }catch(Exception e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
        }finally{
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto,GameSession gameSession,String queryString) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, CredentialNotFoundException, InvalidRequestException {
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

        // Trim the string from the beginning until the first "&sign"
        int signIndex = queryString.indexOf("&sign");
        String trimmedString = queryString.substring(0, signIndex);

        // Convert query string into map format with ASCII order
        MultiValueMap<String, String> sortedMultiValueMap = vendorService.convertToSortedMultiValueMap(trimmedString);

        // generate sign value
        String verify_sign = vendorService.generateSign(sortedMultiValueMap,secret_key);

        ValidationUtils.isEquals(verify_sign, dto.getSign(), InvalidRequestException::new);
    }
}