package com.nextgen.gameaggregator.vendor.bgaming.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.bgaming.vo.TransactionVo;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

            List<TransactionVo> transactionVoList = new ArrayList<>();
            BigDecimal balance = null;
            for (ActionDto actionDto : commonDto.getActions()) {
                RollbackDto rollbackDto = this.setRollbackDto(actionDto, httpRequestLog);
                if (actionDto.getAction().equals("rollback")) {
                    balance = walletService.processRollback(traceId, rollbackDto, gameSession, vendorService);
                }
                TransactionVo transactionVo = new TransactionVo();
                transactionVo.setActionId(actionDto.getActionId());
                transactionVo.setTxId(actionDto.getActionId());
                transactionVo.setProcessedAt(new DateTime(rollbackDto.getVendorSettledTime()).toString());
                transactionVoList.add(transactionVo);
            }

            // Construct VO
            balance = balance.multiply(new BigDecimal(100));
            responseVo.setBalance(balance.intValue());
            responseVo.setGameId(commonDto.getGameId());
            responseVo.setTransactions(transactionVoList);
            responseVo.setHttpStatus(HttpStatus.SC_OK);
        } catch (InvalidSignatureException e) {
            responseVo.setCode(HttpStatus.SC_FORBIDDEN);
            responseVo.setMessage("Request sign doesn't match.");
            responseVo.setHttpStatus(HttpStatus.SC_FORBIDDEN);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (BetRefundIdempotentViolationException |
                 BetResultIdempotentViolationException |
                 TransactionStillProcessingException e) {
            responseVo = handleDuplicateBet(commonDto, httpRequestLog, request);
            responseVo.setHttpStatus(HttpStatus.SC_OK);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledVendorLineException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (CredentialNotFoundException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidAgentApiCredentialException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledAgentPlayerException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledGameException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (RecordNotFoundException | BetNotFoundException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Action id not found.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpStatus = responseVo.getHttpStatus();
            responseVo.setHttpStatus(null);
            httpService.end(httpRequestLog, responseVo);
        }
        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(httpStatus));
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

    private RollbackDto setRollbackDto(ActionDto actionDto, HttpRequestLog httpRequestLog) {
        RollbackDto rollbackDto = new RollbackDto();
        rollbackDto.setBetId(actionDto.getOriginalActionId());
        rollbackDto.setTimestamp(httpRequestLog.getStartTime());

        return rollbackDto;
    }

    private ResponseVo handleDuplicateBet(CommonDto commonDto, HttpRequestLog httpRequestLog, HttpServletRequest request) {
        ResponseVo responseVo = new ResponseVo();
        try {
            responseVo = balanceService.balance(commonDto, httpRequestLog, request);
            List<TransactionVo> transactionVoList = new ArrayList<>();
            if (commonDto.getActions() != null && !commonDto.getActions().isEmpty()) {
                for (ActionDto actionDto : commonDto.getActions()) {
                    TransactionVo transactionVo = new TransactionVo();
                    transactionVo.setActionId(actionDto.getActionId());
                    transactionVo.setTxId(actionDto.getActionId());
                    transactionVoList.add(transactionVo);
                }
            }
            responseVo.setGameId(commonDto.getGameId());
            responseVo.setTransactions(transactionVoList);
        } catch (Exception e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
        }
        return responseVo;
    }
}
