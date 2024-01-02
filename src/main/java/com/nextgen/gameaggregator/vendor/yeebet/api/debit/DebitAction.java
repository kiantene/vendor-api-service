package com.nextgen.gameaggregator.vendor.yeebet.api.debit;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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
            gameSession = gameSessionService.verifyToken(debitDto.getUsername());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(debitDto.getBetsDto().getGameid(),gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto,gameSession);

            // 1 - means normal place bet
            if(debitDto.getType().equals("1")){
                // Process Bet
                BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, httpRequestLog.getRequestBody(), httpRequestLog);

                // set vo
                responseVo.setDesc("Success");
                responseVo.setResult(0);
                responseVo.setSerialnumber(debitDto.getSerialnumber());
                responseVo.setOrderno(traceId);
                responseVo.setBalance(betEvent.getLastBalance().doubleValue());
            }

            // 7 - cancel settled transaction (return error msg to reject the cancel request)
            if(debitDto.getType().equals("7")){
                // set vo
                responseVo.setDesc("Refuse to cancel this transaction");
                responseVo.setResult(-1000);
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
            responseVo.setDesc("The system is error, please contact");
            responseVo.setResult(-1000);
        } catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);

            // Retrieve the latest wallet balance from Operator
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setDesc("Success");
            responseVo.setResult(0);
            responseVo.setSerialnumber(debitDto.getSerialnumber());
            responseVo.setOrderno(traceId);
            responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));
        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc("Insufficient deduce amount");
            responseVo.setResult(-1030);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc("Parameter error,please check");
            responseVo.setResult(-1002);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);

            // set vo
            responseVo.setDesc("The system is error, please contact");
            responseVo.setResult(-1000);

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

    private void doVerification(DebitDto dto,GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException, CurrencyNotSupportedException, AuthenticationException {
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
