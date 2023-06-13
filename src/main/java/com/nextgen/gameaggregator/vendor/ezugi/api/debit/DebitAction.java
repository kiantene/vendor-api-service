package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.constant.BetTypeID;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.service.VendorService;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class DebitAction {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private Environment environment;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.DEBIT)
    public CommonVo debit(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        DebitVo debitVo = new DebitVo();
        try {
            String body = httpRequestLog.getRequestBody();
            DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(debitDto);

            // Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(debitDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto, gameSession, httpRequestLog, request);

            // Get walletBalance
            BigDecimal balance = BigDecimal.ZERO;
            switch (debitDto.getBetTypeID()) {
                case BetTypeID.DEBIT_TIP:
                    balance = walletService.processBetResult(traceId, gameSession, debitDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                    break;
                default:
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, body);
                    balance = betEvent.getLastBalance();
                    break;
            }
            // Construct Vo
            debitVo.setToken(debitDto.getToken());
            debitVo.setOperatorId(debitDto.getOperatorId());
            debitVo.setUid(gameSession.getVendorPlayerUsername());
            debitVo.setRoundId(debitDto.getVendorRoundId());
            debitVo.setTransactionId(debitDto.getTransactionId());
            debitVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            debitVo.setCurrency(gameSession.getVendorCurrencyCode());
            debitVo.setErrorCode(ResponseCodes.OK);
            debitVo.setTimestamp(System.currentTimeMillis());
        } catch (AuthenticationException e) {
            debitVo.setErrorCode(ResponseCodes.TOKEN_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            debitVo.setErrorCode(ResponseCodes.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            debitVo.setErrorCode(ResponseCodes.USER_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            debitVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            debitVo.setErrorDescription("Invalid Hash");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            debitVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            if (e.getValidation() != null) {
                String violation = e.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCodes.RESPONSE_DESCRIPTION.get(debitVo.getErrorCode())); // if there's no value, set it to the default invalid request parameter
                debitVo.setErrorDescription(violation);
            }
            httpService.logError(httpRequestLog, e);
        } catch (InvalidFormatException e) {
            debitVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            debitVo.setErrorDescription("Invalid Bet Type");
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e){
            debitVo.setErrorCode(ResponseCodes.OK);
            debitVo.setErrorDescription("Transaction already processed");
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException | DisabledGameException |
                 MergedBetDataIntegrityException | DisabledAgentPlayerException |
                 InvalidAgentApiCredentialException |
                 DisabledVendorLineException | CredentialNotFoundException | InvalidKeyException |
                 CouchbaseDataIntegrityException | NoSuchAlgorithmException | InvalidOperatorResponseException e) {
            debitVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if(debitVo.getErrorDescription()==null) {
                debitVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(debitVo.getErrorCode()));
            }
            httpService.end(httpRequestLog, debitVo);
        }
        return debitVo;
    }

    private void doValidation(DebitDto debitDto) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        // General validation
        ValidationUtils.validateRequest(debitDto);
    }

    private void doVerification(DebitDto debitDto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request)
            throws AuthenticationException, InvalidPlayerException, CredentialNotFoundException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidSignatureException, NoSuchAlgorithmException, InvalidKeyException, InvalidFormatException {
        // Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), debitDto.getTableId(), AuthenticationException::new);

        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, debitDto.getUid());

        // Verify Signature key from vendor given
        String hashKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.HASH_KEY);
        VendorService.verifyHash(hashKey, httpRequestLog.getRequestBody(), request.getHeader("hash"));

        // Verify valid bet type id
        VendorService.verifyDebitBetTypeId(debitDto.getBetTypeID());
    }
}
