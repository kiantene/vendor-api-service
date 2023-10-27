package com.nextgen.gameaggregator.vendor.iloveu.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.iloveu.api.settle.SettleTransactionDto;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Credentials;
import com.nextgen.gameaggregator.vendor.iloveu.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.iloveu.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.iloveu.service.VendorService;
import com.nextgen.gameaggregator.vendor.iloveu.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.iloveu.vo.DataVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.BET)
    public List<CommonVo> bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Construct VO
        BetVo betVo = new BetVo();
        List<CommonVo> responseVo = new ArrayList<>();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            body = "{ \"transactions\" :" + body + "}";

            //Convert original request body into balanceDto
            BetDto betDto = HttpService.convertJsonToDto(body, BetDto.class);

            List<CompletableFuture<CommonVo>> futures = new LinkedList<>();
            for (BetTransactionDto transaction : betDto.getTransactions()) {

                CompletableFuture<CommonVo> future = CompletableFuture.supplyAsync(() -> processData(transaction, request));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<CommonVo> transactionsList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            betVo.setTransactions(transactionsList);
            responseVo.addAll(transactionsList);

        } catch (JsonProcessingException jsonProcessingException) {
            CommonVo commonVo = new CommonVo();
            commonVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            responseVo.add(commonVo);
            httpService.logError(httpRequestLog, jsonProcessingException);

        } catch (Exception exception) {
            CommonVo commonVo = new CommonVo();
            commonVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            responseVo.add(commonVo);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, betVo);

        }

        return responseVo;
    }

    private void doValidation(BetTransactionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.isEquals("Bet", dto.getMethod(), InvalidRequestException::new);

        if (!vendorService.isValidDateTime(dto.actionDate)) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(BetTransactionDto dto, GameSession gameSession) throws
            InvalidPlayerException,
            AuthenticationException,
            DisabledAgentPlayerException,
            DisabledGameException,
            DisabledVendorLineException,
            InvalidRequestException,
            InvalidEncryptionException,
            CredentialNotFoundException {

        //Verify vendor SN
        String sn = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SERIAL_NUMBER);
        ValidationUtils.isEquals(sn, dto.getSn(), InvalidRequestException::new);

        //Generate encryptString
        String apiKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_SECRET_KEY);
        String encryptString = dto.getId() + dto.getMethod() + dto.getSn() + dto.getLoginId() + apiKey;

        //Verify signature
        String md5Param = vendorService.md5(encryptString);
        if (!dto.getSignature().toUpperCase().equals(md5Param.toUpperCase())) {
            throw new InvalidRequestException(vendorService.invalidRequestRespond(ResponseCodes.INVALID_SIGNATURE));
        }

        //Validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getLoginId());


    }

    private CommonVo processData(BetTransactionDto betTransactionDto, HttpServletRequest request) {
        CommonVo commonVo = new CommonVo();
        GameSession gameSession = null;

        //Generate new traceId
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();

        try {
            // 1. Validate each user data
            this.doValidation(betTransactionDto);

            // 2. Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betTransactionDto.getLoginId().toLowerCase(), betTransactionDto.getGameName().toLowerCase());

            // 3. Verify Credential and Currency
            this.doVerification(betTransactionDto, gameSession);

            // 4. Process Result
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betTransactionDto, body, httpRequestLog);

            // 5. Set transactionsVo
            commonVo.getDataVo().setBalance(betEvent.getLastBalance().setScale(2, RoundingMode.DOWN));

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            commonVo.setCode(ResponseCodes.REPEATED_REQUEST);
            commonVo.getDataVo().setBalance(betResultIdempotentViolationException.getBalance().setScale(2, RoundingMode.DOWN));

        } catch (AuthenticationException |
                 InvalidEncryptionException |
                 DisabledVendorLineException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException |
                 InvalidPlayerException |
                 DisabledAgentPlayerException |
                 VendorCurrencyNotSupportException |
                 DisabledGameException generalException) {
            commonVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            httpService.logError(httpRequestLog, generalException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            commonVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (TransactionStillProcessingException | CouchbaseDataIntegrityException systemErrorException) {
            commonVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, systemErrorException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            //SC_INSUFFICIENT_FUNDS
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                commonVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            } else {
                commonVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            }
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidRequestException invalidRequestException) {
            //return error message according param
            if (invalidRequestException.getValidation() != null) {
                commonVo.setResponseCode(
                        invalidRequestException.getValidation()
                                .entrySet()
                                .stream()
                                .findFirst()
                                .map(Map.Entry::getValue) // get the value of the first element
                                .orElse(ResponseCodes.INVALID_PARAMETER)
                );
            } else {
                commonVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (Exception exception) {
            commonVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

}
