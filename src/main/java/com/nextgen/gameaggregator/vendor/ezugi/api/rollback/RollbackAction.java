package com.nextgen.gameaggregator.vendor.ezugi.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ezugi.service.VendorService;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollbackAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.ROLLBACK)
    public CommonVo rollback(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        RollbackVo rollbackVo = new RollbackVo();
        try {
            String body = httpRequestLog.getRequestBody();
            RollbackDto rollbackDto = HttpService.convertJsonToDto(body, RollbackDto.class);

            // Validate request parameters (Non-database calls)
            this.doValidation(rollbackDto);

            // Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(rollbackDto.getToken());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(rollbackDto, gameSession, httpRequestLog, request);

            // Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService);

            // Construct Vo
            rollbackVo.setToken(rollbackDto.getToken());
            rollbackVo.setOperatorId(rollbackDto.getOperatorId());
            rollbackVo.setUid(gameSession.getVendorPlayerUsername());
            rollbackVo.setRoundId(rollbackDto.getRoundId());
            rollbackVo.setTransactionId(rollbackDto.getTransactionId());
            rollbackVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            rollbackVo.setCurrency(gameSession.getVendorCurrencyCode());
            rollbackVo.setErrorCode(ResponseCodes.OK);
            rollbackVo.setTimestamp(System.currentTimeMillis());
        } catch (AuthenticationException e) {
            rollbackVo.setErrorCode(ResponseCodes.TOKEN_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            rollbackVo.setErrorCode(ResponseCodes.TRANSACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (BetRefundIdempotentViolationException e){
            rollbackVo.setErrorCode(ResponseCodes.OK);
            rollbackVo.setErrorDescription("Transaction already processed");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidSignatureException e) {
            rollbackVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            rollbackVo.setErrorDescription("Invalid Hash");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidFormatException e) {
            rollbackVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            rollbackVo.setErrorDescription("Invalid Amount");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            rollbackVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            rollbackVo.setErrorDescription("Invalid parameter");
            httpService.logError(httpRequestLog, e);
        } catch (DisabledGameException |
                 DisabledAgentPlayerException | CurrencyNotSupportedException | RecordNotFoundException |
                 InvalidPlayerException | InvalidAgentApiCredentialException | CredentialNotFoundException |
                 DisabledVendorLineException | InvalidKeyException | CouchbaseDataIntegrityException |
                 NoSuchAlgorithmException | InvalidOperatorResponseException e) {
            rollbackVo.setErrorCode(ResponseCodes.GENERAL_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if(rollbackVo.getErrorDescription()==null) {
                rollbackVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION_ROLLBACK.get(rollbackVo.getErrorCode()));
            }
            httpService.end(httpRequestLog, rollbackVo);
        }
        return rollbackVo;
    }

    private void doValidation(RollbackDto rollbackdto) throws InvalidRequestException {
        ValidationUtils.validateRequest(rollbackdto);
    }

    private void doVerification(RollbackDto rollbackdto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws DisabledVendorLineException, DisabledAgentPlayerException, CurrencyNotSupportedException, InvalidPlayerException, DisabledGameException, AuthenticationException, InvalidSignatureException, NoSuchAlgorithmException, InvalidKeyException, CredentialNotFoundException, BetNotFoundException, InvalidFormatException, InvalidRequestException {
        // validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, rollbackdto.getUid());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), rollbackdto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify Operator Id from vendor given
        String operatorId = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.OPERATOR_ID);
        ValidationUtils.isEquals(operatorId, String.valueOf(rollbackdto.getOperatorId()), InvalidRequestException::new);

        // Verify Signature key from vendor given
        String hashKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.HASH_KEY);
        VendorService.verifyHash(hashKey, httpRequestLog.getRequestBody(), request.getHeader("hash"));

        // validate rollback amount and debit amount is tally
        vendorService.verifyRollbackAmount(rollbackdto, gameSession);
    }
}
