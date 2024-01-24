package com.nextgen.gameaggregator.vendor.bgaming.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bgaming.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.bgaming.vo.TransactionVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RollbackAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private BalanceService balanceService;

    @PostMapping(path = EndPoints.ROLLBACK)
    public ResponseEntity<ResponseVo> rollback(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();
        ResponseVo responseVo = new ResponseVo();
        Integer httpStatus;
        CommonDto commonDto = null;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);

            // Validate the commonDto object
            this.doValidation(commonDto);

            // Get vendor player details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getUserId());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, gameSession, httpRequestLog, request);

            RollbackDto rollbackDto = new ModelMapper().map(commonDto, RollbackDto.class);
            BigDecimal balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService, httpRequestLog);

            // Construct VO
            for (ActionDto actionDto : commonDto.getActions()) {
                TransactionVo transactionVo = new TransactionVo();
                transactionVo.setActionId(actionDto.getActionId());
                transactionVo.setTxId(actionDto.getActionId());
                transactionVo.setProcessedAt(new DateTime(rollbackDto.getVendorSettledTime()).toString());
                responseVo.addTransactions(transactionVo);
            }
            responseVo.setBalance(balance.intValue());
            responseVo.setGameId(commonDto.getVendorRoundId());
            responseVo.setHttpStatus(HttpStatus.SC_OK);
        } catch (InvalidSignatureException e) {
            responseVo.setResponseCodes(ResponseCodes.REQUEST_SIGN_DOES_NOT_MATCH);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException |
                 DisabledVendorLineException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 DisabledAgentPlayerException |
                 DisabledGameException |
                 InvalidRequestException |
                 InvalidPlayerException |
                 JsonProcessingException |
                 TransactionStillProcessingException |
                 InvalidOperatorResponseException e) {
            responseVo.setResponseCodes(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException e) {
            handleDuplicateBet(commonDto, httpRequestLog, request, responseVo);
            responseVo.setHttpStatus(HttpStatus.SC_OK);
            httpService.logError(httpRequestLog, e);
        } catch (RecordNotFoundException | BetNotFoundException e) {
            responseVo.setResponseCodes(ResponseCodes.BET_ACTION_NOT_FOUND);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCodes(ResponseCodes.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(responseVo.getHttpStatus()));
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CommonDto commonDto, GameSession gameSession, HttpRequestLog httpRequestLog, HttpServletRequest request) throws InvalidPlayerException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException, JsonProcessingException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, commonDto.getUserId());

        // Convert Body to Map for signature check
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> bodyObj = mapper.readValue(httpRequestLog.getRequestBody(), Map.class);

        // Verify Signature key from vendor given
        String authToken = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AUTH_TOKEN);
        VendorService.verifySign(authToken, new Gson().toJson(bodyObj), request.getHeader("X-REQUEST-SIGN"));
    }

    private void handleDuplicateBet(CommonDto commonDto, HttpRequestLog httpRequestLog, HttpServletRequest request, ResponseVo responseVo) {
        try {
            balanceService.balance(commonDto, httpRequestLog, request, responseVo);
        } catch (Exception e) {
            responseVo.setResponseCodes(ResponseCodes.UNKNOWN_ERROR);
        }
    }
}
