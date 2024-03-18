package com.nextgen.gameaggregator.vendor.bombay.api.debit;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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
import java.util.Map;

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

    @PostMapping(path = EndPoints.DEBIT)
    public ResponseVo debit(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        DebitDto debitDto = null;

        GameSession gameSession = new GameSession();

        try{
            String body = httpRequestLog.getRequestBody();

            // get x-signature value for validation
            Map<String,String> header = vendorService.headersToHashMap(request);

            debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(debitDto);

            // Verify session token and generate update game session while playing others game
            gameSession = gameSessionService.verifyToken(debitDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto, gameSession, header.get("x-signature"), body);

            // Process Bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, httpRequestLog.getRequestBody(), httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(betEvent.getLastBalance().intValue());
            responseVo.setCurrency(gameSession.getCurrencyCode());

        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(InsufficientBalanceException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_NOT_ENOUGH_MONEY);
        } catch(InvalidSignatureException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_SIGNATURE);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);
            if (e.getValidation() != null) {
                String violation = e.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCodes.RS_ERROR_UNKNOWN); // if there's no value, set it to the default value
                responseVo.setStatus(violation);
            }
        } catch(InvalidPlayerException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_USER);
        } catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            // bet request will only send in one time and will trigger rollback once it failed to process
            httpService.logError(httpRequestLog, e);

            BigDecimal balance = getCurrentBalance(traceId, gameSession, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
            responseVo.setUser(gameSession.getVendorPlayerUsername());
            responseVo.setBalance(balance.intValue());
            responseVo.setCurrency(gameSession.getCurrencyCode());

        } catch(InvalidAgentApiCredentialException |
                VendorCurrencyNotSupportException |
                DisabledAgentPlayerException |
                DisabledGameException |
                InvalidOperatorResponseException |
                DisabledVendorLineException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally{
            responseVo.setRequest_uuid(debitDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(DebitDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(DebitDto dto,GameSession gameSession, String x_signature, String request_body) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, GameNotSupportedException, CredentialNotFoundException, InvalidSignatureException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor's x-signature
        String convertedJsonString = vendorService.convertObjectMapper(request_body); // Convert json beautified format back to compact JSON string

//        String vendor_public_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.vendor_public_key);
//        Boolean validateSignature = vendorService.validateSignature(x_signature, convertedJsonString, vendor_public_key);
//
//        // validateSignature not equal to true mean credential problem or this data is not from vendor
//        if(!validateSignature){
//            throw new InvalidSignatureException();
//        }
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
