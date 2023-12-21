package com.nextgen.gameaggregator.vendor.yeebet.api.credit;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.yeebet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yeebet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yeebet.service.VendorService;
import com.nextgen.gameaggregator.vendor.yeebet.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class CreditAction {
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

    @PostMapping(path = EndPoints.DEPOSIT)
    public ResponseEntity<ResponseVo> credit(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        GameSession gameSession = new GameSession();

        BigDecimal balance = null;

        ResponseVo responseVo = new ResponseVo();

        CreditDto creditDto = new CreditDto();

        Integer httpStatus = HttpStatus.SC_OK; //default is 200 status

        try {

            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            creditDto = httpService.convertQueryStringToDtoUrlDecode(body,CreditDto.class);

            creditDto.convertBetToDto();

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(creditDto.getUsername());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto,gameSession);

            // 9 - request to update those unsettled transaction to settled status
            if(creditDto.getType().equals("9")){
                ResultType resultType = vendorService.calculateResultType(creditDto.getBetAmount(),creditDto.getWinAmount(),creditDto.getJackpotAmount(),false);

                balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

                // set vo
                responseVo.setDesc("Success");
                responseVo.setResult(0);
                responseVo.setSerialnumber(creditDto.getSerialnumber());
                responseVo.setOrderno(traceId);
                responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));
            }

            // 7 - cancel settled transaction (return error msg to reject the cancel request)
            if(creditDto.getType().equals("7")){
                // set vo
                responseVo.setDesc("Refuse to cancel this transaction");
                responseVo.setResult(-1000);
            }

        } catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);

            // Retrieve the latest wallet balance from Operator
            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            // set vo
            responseVo.setDesc("Success");
            responseVo.setResult(0);
            responseVo.setSerialnumber(creditDto.getSerialnumber());
            responseVo.setOrderno(traceId);
            responseVo.setBalance(Double.valueOf(balance.setScale(2, RoundingMode.DOWN).toString()));

        } catch(VendorCurrencyNotSupportException |
                AuthenticationException |
                InsufficientBalanceException |
                InvalidOperatorResponseException |
                DisabledVendorLineException |
                InvalidAgentApiCredentialException |
                InvalidPlayerException |
                DisabledAgentPlayerException |
                MergedBetDataIntegrityException |
                DisabledGameException |
                BetNotFoundException e){
            httpService.logError(httpRequestLog, e);

            httpStatus = HttpStatus.SC_FORBIDDEN; //403 status

            // set vo
            responseVo.setDesc("The system is error, please contact");
            responseVo.setResult(-1000);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);

            httpStatus = HttpStatus.SC_FORBIDDEN; //403 status

            // set vo
            responseVo.setDesc("Parameter error,please check");
            responseVo.setResult(-1002);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);

            httpStatus = HttpStatus.SC_FORBIDDEN; //403 status

            // set vo
            responseVo.setDesc("The system is error, please contact");
            responseVo.setResult(-1000);
        }finally{
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(httpStatus));
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        //check object inside the dto
        ValidationUtils.validateRequest(dto.getBetsDto());
    }

    private void doVerification(CreditDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, GameNotSupportedException, CurrencyNotSupportedException, CredentialNotFoundException, InvalidRequestException {

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
