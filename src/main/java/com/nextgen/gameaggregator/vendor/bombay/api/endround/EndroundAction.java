package com.nextgen.gameaggregator.vendor.bombay.api.endround;

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
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(path= EndPoints.PATH)
@Slf4j
public class EndroundAction {
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
    @Autowired
    private RedissonService redissonService;

    @PostMapping(path = EndPoints.END_ROUND)
    public ResponseVo credit(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        EndroundDto endroundDto = null;

        GameSession gameSession = new GameSession();

        RLock userLock = null;

        try{
            String body = httpRequestLog.getRequestBody();

            // get x-signature value for validation
            Map<String,String> header = vendorService.headersToHashMap(request);

            endroundDto = HttpService.convertJsonToDto(body, EndroundDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(endroundDto);

            // Verify session token
            gameSession = gameSessionService.verifyToken(endroundDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(endroundDto, gameSession, header.get("x-signature"), body);

            Thread.sleep(150);

            userLock = redissonService.getRedissonClient().getLock("RedissonLock:BOMBAY:" + endroundDto.getRound());

            // if it is not lock meant is lose
            if(!userLock.isLocked()){
                // this end-point just handle transaction with end status, so set it as result end
                ResultType resultType = ResultType.END;

                // no need to return wallet balance
                BigDecimal balance = walletService.processBetResult(traceId, gameSession, endroundDto, resultType, vendorService, httpRequestLog);
            }

            responseVo.setStatus(ResponseCodes.RS_OK);
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
        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(InvalidPlayerException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_USER);
        } catch(InvalidSignatureException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_SIGNATURE);
        } catch(BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_DUPLICATE_TRANSACTION);
        } catch(VendorCurrencyNotSupportException |
                InsufficientBalanceException |
                InvalidOperatorResponseException |
                DisabledVendorLineException |
                InvalidAgentApiCredentialException |
                DisabledAgentPlayerException |
                MergedBetDataIntegrityException |
                TransactionStillProcessingException |
                DisabledGameException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } catch(Exception e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_UNKNOWN);
        } finally{
            // Release the lock if we acquired it and still hold it
            if (userLock != null && userLock.isHeldByCurrentThread()) {
                userLock.unlock();
            }

            responseVo.setRequest_uuid(endroundDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(EndroundDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(EndroundDto dto, GameSession gameSession, String x_signature, String request_body) throws GameNotSupportedException, InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidSignatureException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUser());

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor's x-signature
        String convertedJsonString = vendorService.convertObjectMapper(request_body); // Convert json beautified format back to compact JSON string

        String vendor_public_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.vendor_public_key);
        Boolean validateSignature = vendorService.validateSignature(x_signature, convertedJsonString, vendor_public_key);

        // validateSignature not equal to true mean credential problem or this data is not from vendor
        if(!validateSignature){
            throw new InvalidSignatureException();
        }
    }
}
