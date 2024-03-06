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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

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

    @PostMapping(path = EndPoints.END_ROUND)
    public ResponseVo credit(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        ResponseVo responseVo = new ResponseVo();

        EndroundDto endroundDto = null;

        GameSession gameSession = new GameSession();

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

            // this end-point just handle transaction with end status, so set it as result end
            ResultType resultType = ResultType.END;

            // no need to return wallat balance
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, endroundDto, resultType, vendorService, httpRequestLog);

            responseVo.setStatus(ResponseCodes.RS_OK);
        } catch(InvalidRequestException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_WRONG_SYNTAX);
        } catch(AuthenticationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_TOKEN);
        } catch(InvalidPlayerException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_USER);
        } catch(InvalidSignatureException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_ERROR_INVALID_SIGNATURE);
        } catch(TransactionStillProcessingException |
                BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);
            responseVo.setStatus(ResponseCodes.RS_OK);
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
            responseVo.setRequest_uuid(endroundDto.getRequest_uuid());
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(EndroundDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(EndroundDto dto, GameSession gameSession, String x_signature, String request_body) throws GameNotSupportedException, CurrencyNotSupportedException, InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidSignatureException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());

        // Verify vendor gameCode
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());
        ValidationUtils.isEquals(game_code, dto.getGameId(), GameNotSupportedException::new);

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor's x-signature
        String vendor_public_key = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.vendor_public_key);
        Boolean validateSignature = vendorService.validateSignature(x_signature, request_body, vendor_public_key);

        // validateSignature not equal to true mean credential problem or this data is not from vendor
        if(!validateSignature){
            throw new InvalidSignatureException();
        }
    }
}
