package com.nextgen.gameaggregator.vendor.queenmaker.api.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class RewardAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.WALLET_REWARD)
    public RewardVo RewardAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        RewardVo rewardVo = new RewardVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);

            String body = httpRequestLog.getRequestBody();
            RewardDto rewardDto = HttpService.convertJsonToDto(body, RewardDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(rewardDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<TransactionsVo> transactionsList = new ArrayList<>();
            for (RewardTransactionsDto transaction : rewardDto.getTransactions()) {
                TransactionsVo transactionsVo = processData(transaction, clientId, clientSecret, request);
                transactionsList.add(transactionsVo);
            }
            rewardVo.setTransactions(transactionsList);

        } catch (InvalidRequestException e) {
            rewardVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Request");
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException e) {
            rewardVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Body Format");
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            rewardVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, rewardVo);
        }

        return rewardVo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RewardTransactionsDto rewardTransactionsDto, GameSession gameSession, String clientId, String clientSecret) throws CredentialNotFoundException, InvalidVendorLineException, CurrencyNotSupportedException {
        // 1. Validate Credentials
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        Optional.ofNullable(CLIENT_ID).orElseThrow(CredentialNotFoundException::new);
        Optional.ofNullable(CLIENT_SECRET).orElseThrow(CredentialNotFoundException::new);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 2. Validate Vendor Currency Code
        ValidationUtils.isEquals(rewardTransactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);
    }

    private TransactionsVo processData(RewardTransactionsDto rewardTransactionsDto, String clientId, String clientSecret, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        TransactionsVo transactionsVo = new TransactionsVo();
        GameSession gameSession = null;

        try {
            // 1. Validate each user data
            this.doValidation(rewardTransactionsDto);

            // 2. Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(rewardTransactionsDto.getUserid());

            // 3. Verify Credential and Currency
            this.doVerification(rewardTransactionsDto, gameSession, clientId, clientSecret);

            // 4. Process Reward Result
            BigDecimal balance;
            ResultType resultType = ResultType.BET_WIN;
            balance = walletService.processBetResult(traceId, gameSession, rewardTransactionsDto, resultType, vendorService, httpRequestLog);

            // 5. Set transactionsVo
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(rewardTransactionsDto.getPtxid());
            transactionsVo.setBal(balance);
            transactionsVo.setCur(gameSession.getVendorCurrencyCode());
            transactionsVo.setDup(false);

        } catch (AuthenticationException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_OR_EXPIRED_TOKEN);
            httpService.logError(httpRequestLog, e);
        } catch (CurrencyNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.CURRENCY_MISMATCH);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            transactionsVo.setResponseCode(ResponseCodes.INSUFFICIENT_FUNDS);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            transactionsVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidVendorLineException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException e) {
            transactionsVo.setResponseCode(ResponseCodes.OPERATION_FAILED_DETERMINISTICALLY);
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            // return current balance
            transactionsVo.setBal(this.getBalance(traceId, gameSession));
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(rewardTransactionsDto.getPtxid());
            transactionsVo.setCur(gameSession.getVendorCurrencyCode());
            transactionsVo.setDup(true);
        } catch (TransactionStillProcessingException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Transaction Still Processing");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Processing Error");
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(rewardTransactionsDto.getPtxid());
            transactionsVo.setResponseCode(ResponseCodes.TRANSACTION_DOES_NOT_EXIST);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, transactionsVo);
        }
        return transactionsVo;
    }

    private BigDecimal getBalance(String traceId, GameSession gameSession) {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            balance = walletService.getBalance(traceId, gameSession, null);
        } catch (Exception e) {
            // nothing to do
        }
        return balance;
    }
}
