package com.nextgen.gameaggregator.vendor.bgaming.api.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.api.balance.BalanceService;
import com.nextgen.gameaggregator.vendor.bgaming.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.bgaming.api.endround.EndRoundService;
import com.nextgen.gameaggregator.vendor.bgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bgaming.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.bgaming.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.bgaming.vo.TransactionVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BetService betService;
    @Autowired
    private EndRoundService endRoundService;

    @PostMapping(path = EndPoints.PLAY)
    public ResponseEntity<ResponseVo> action(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

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

            // Handle the action and return the resulting value
            responseVo = this.serviceHandling(commonDto, httpRequestLog, request);

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
        } catch (InsufficientBalanceException e) {
            responseVo.setCode(HttpStatus.SC_CONTINUE);
            responseVo.setMessage("Funds not enough.");
            responseVo.setHttpStatus(HttpStatus.SC_PRECONDITION_FAILED);
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
        } catch (BetNotFoundException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException | MergedBetDataIntegrityException | CouchbaseDataIntegrityException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Unknown error.");
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (TransactionStillProcessingException | BetResultIdempotentViolationException e) {
            responseVo = handleDuplicateBet(commonDto, httpRequestLog, request);
            responseVo.setHttpStatus(HttpStatus.SC_OK);
            httpService.logError(httpRequestLog, e);
        } catch (CurrencyNotSupportedException e) {
            responseVo.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            responseVo.setMessage("Currency not support (maybe need integrate new currency code for handle convert amount).");
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

    private ResponseVo serviceHandling(CommonDto commonDto, HttpRequestLog httpRequestLog, HttpServletRequest request) throws InvalidRequestException, InvalidAgentApiCredentialException, InvalidPlayerException, AuthenticationException, BetResultIdempotentViolationException, DisabledAgentPlayerException, DisabledGameException, InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException, CouchbaseDataIntegrityException, DisabledVendorLineException, MergedBetDataIntegrityException, BetNotFoundException, InvalidSignatureException, CredentialNotFoundException, JsonProcessingException, CurrencyNotSupportedException, GameNotSupportedException {
        ResponseVo responseVo = new ResponseVo();

        if (!commonDto.getFinished() && (commonDto.getActions() == null || commonDto.getActions().isEmpty())) {
            // No action , Get balance
            responseVo = balanceService.balance(commonDto, httpRequestLog, request);
        } else if (commonDto.getFinished() && (commonDto.getActions() == null || commonDto.getActions().isEmpty())) {
            TransactionVo transactionVo = endRoundService.endRound(commonDto, null, httpRequestLog, request);
            responseVo.setBalance(transactionVo.getBalance());
            responseVo.setGameId(commonDto.getGameId());
        } else {
            int count = 0;
            List<TransactionVo> transactionVoList = new ArrayList<>();
            for (ActionDto actionDto : commonDto.getActions()) {
                TransactionVo transactionVo;
                count++;
                switch (actionDto.getAction()) {
                    case "bet" -> {
                        transactionVo = betService.bet(commonDto, actionDto, httpRequestLog, count, request);
                    }
                    case "win" -> {
                        transactionVo = endRoundService.endRound(commonDto, actionDto, httpRequestLog, request);
                    }
                    default -> {
                        throw new InvalidRequestException();
                    }
                }
                responseVo.setBalance(transactionVo.getBalance());
                transactionVo.setBalance(null);
                transactionVoList.add(transactionVo);
            }
            responseVo.setGameId(commonDto.getGameId());
            responseVo.setTransactions(transactionVoList);
        }
        return responseVo;
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
