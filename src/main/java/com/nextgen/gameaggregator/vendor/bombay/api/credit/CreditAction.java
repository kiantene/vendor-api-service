package com.nextgen.gameaggregator.vendor.bombay.api.credit;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
    @Autowired
    ValidationService validationService;

    @PostMapping(path = EndPoints.CREDIT)
    public ResponseVo credit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        CreditDto creditDto = null;

        GameSession gameSession = new GameSession();

        BigDecimal balance = null;

        try{
            String body = httpRequestLog.getRequestBody();

            // get x-signature value for validation
            Map<String,String> header = vendorService.headersToHashMap(request);

            creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(creditDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(creditDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession, header.get("x-signature"), body);

            // this end-point just handle transaction with win status, so set it as result win
            ResultType resultType = ResultType.WIN;

            balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(balance.intValue());
            responseVo.setCurrency(gameSession.getCurrencyCode());
        } catch(BetNotFoundException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_TRANSACTION_DOES_NOT_EXIST);
        } catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);

            balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(balance.intValue());
            responseVo.setCurrency(gameSession.getCurrencyCode());
        } catch(GameNotSupportedException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_GAME);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_WRONG_SYNTAX);
        } catch(InvalidPlayerException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_USER);
        } catch(InvalidSignatureException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_SIGNATURE);
        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(VendorCurrencyNotSupportException |
                InsufficientBalanceException |
                InvalidOperatorResponseException |
                DisabledVendorLineException |
                InvalidAgentApiCredentialException |
                DisabledAgentPlayerException |
                MergedBetDataIntegrityException |
                DisabledGameException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally{
            responseVo.setRequest_uuid(creditDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CreditDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession, String x_signature, String request_body) throws DisabledGameException, DisabledAgentPlayerException, InvalidPlayerException, DisabledVendorLineException, GameNotSupportedException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor's x-signature
        request_body = request_body.replaceAll("\\s", ""); // Remove all space, \n or \r
        String vendor_public_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.vendor_public_key);
        Boolean validateSignature = vendorService.validateSignature(x_signature, request_body, vendor_public_key);

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
