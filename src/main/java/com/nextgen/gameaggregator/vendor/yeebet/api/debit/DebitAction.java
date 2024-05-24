package com.nextgen.gameaggregator.vendor.yeebet.api.debit;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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
public class DebitAction {
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
    @Autowired
    private WalletAdjustmentService walletAdjustmentService;
    @Autowired
    private ValidationService validationService;
    
    @PostMapping(path = EndPoints.DEDUCT)
    public ResponseVo debit(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        GameSession gameSession = new GameSession();

        ResponseVo responseVo = new ResponseVo();

        DebitDto debitDto = new DebitDto();

        BigDecimal balance = BigDecimal.ZERO;

        try{
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            debitDto = httpService.convertQueryStringToDtoUrlDecode(body, DebitDto.class);

            // convert query string to Dto
            debitDto.convertBetToDto();

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(debitDto);

            // Verify session token and generate update game session while playing others game
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(debitDto.getUsername());

            // check db game code is stg or not
            if(gameSession.getVendorGameCode().toLowerCase().contains("_stg")){
                debitDto.getBetsDto().setGameid(debitDto.getBetsDto().getGameid() + "_stg");
            }

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(debitDto.getBetsDto().getGameid(),gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto,gameSession,body);

            // 1 - means normal place bet
            if(debitDto.getType().equals(RequestType.BET_REQUEST)){
                // Process Bet
                BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, httpRequestLog.getRequestBody(), httpRequestLog);

                // set vo
                responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
                responseVo.setResult(ResponseCodes.SUCCESS_CODE);
                responseVo.setSerialnumber(debitDto.getSerialnumber());
                responseVo.setOrderno(traceId);
                responseVo.setBalance(betEvent.getLastBalance().doubleValue());
            }

            // 7 - cancel settled transaction (return error msg to reject the cancel request)
            if(debitDto.getType().equals(RequestType.CANCEL_REQUEST)){
                // set vo
                responseVo.setDesc(ResponseCodes.REJECT_CANCEL_MSG);
                responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
            }

        } catch(InvalidAgentApiCredentialException |
               InvalidPlayerException |
               VendorCurrencyNotSupportException |
               AuthenticationException |
               DisabledAgentPlayerException |
               DisabledGameException |
               InvalidOperatorResponseException |
               DisabledVendorLineException e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);
        } catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);

            // Retrieve the latest wallet balance from Operator
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setDesc(ResponseCodes.SUCCESS_MSG);
            responseVo.setResult(ResponseCodes.SUCCESS_CODE);
            responseVo.setSerialnumber(debitDto.getSerialnumber());
            responseVo.setOrderno(traceId);
            responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.INSUFFICIENT_MSG);
            responseVo.setResult(ResponseCodes.INSUFFICIENT_ERROR_CODE);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.PARAMETER_ERROR_MSG);
            responseVo.setResult(ResponseCodes.PARAMETER_ERROR_CODE);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc(ResponseCodes.SYSTEM_ERROR_MSG);
            responseVo.setResult(ResponseCodes.SYSTEM_ERROR_CODE);

        }finally{
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(DebitDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getBetsDto());
    }

    private void doVerification(DebitDto dto,GameSession gameSession,String queryString) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException, CurrencyNotSupportedException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession,dto.getUsername());

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUsername(), InvalidPlayerException::new);

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getBetsDto().getGameid(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getBetsDto().getCurrency(), CurrencyNotSupportedException::new);

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

    private BigDecimal getCurrentBalance(String traceId, GameSession gameSession, HttpRequestLog httpRequestLog) {
        BigDecimal balance = BigDecimal.ZERO;

        try {
            balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

        } catch (Exception exception) {

        }

        return balance;
    }
}
