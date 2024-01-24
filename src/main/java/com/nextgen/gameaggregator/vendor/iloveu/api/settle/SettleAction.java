package com.nextgen.gameaggregator.vendor.iloveu.api.settle;

import com.couchbase.client.core.deps.com.google.gson.Gson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.iloveu.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Credentials;
import com.nextgen.gameaggregator.vendor.iloveu.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.iloveu.constant.GameType;
import com.nextgen.gameaggregator.vendor.iloveu.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.iloveu.service.VendorService;
import com.nextgen.gameaggregator.vendor.iloveu.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
public class SettleAction {

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

    @PostMapping(path = EndPoints.SETTLE)
    public List<CommonVo> bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        // Construct VO
        SettleVo settleVo = new SettleVo();
        List<CommonVo> responseVo = new ArrayList<>();

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();
            body = "{ \"transactions\" :" + body + "}";

            //Convert original request body into balanceDto
            SettleDto settleDto = vendorService.convertJsonToDto(body, SettleDto.class);

            //Loop bet/settle record and process in asynchronous
            List<CompletableFuture<CommonVo>> settles = new LinkedList<>();
            for (SettleTransactionDto transaction : settleDto.getTransactions()) {
                CompletableFuture<CommonVo> settle = CompletableFuture.supplyAsync(() -> processData(transaction, request));
                settles.add(settle);
            }
            //Process loop response
            List<CommonVo> transactionsList = vendorService.processMultipleDataResponds(settles);
            settleVo.setTransactions(transactionsList);
            responseVo.addAll(transactionsList);


        } catch (JsonProcessingException jsonProcessingException) {
            CommonVo commonVo = new CommonVo();
            commonVo.setResponseCode(ResponseCodes.INVALID_PARAMETER);
            responseVo.add(commonVo);
            settleVo.setTransactions(responseVo);
            httpService.logError(httpRequestLog, jsonProcessingException);

        } catch (Exception exception) {
            CommonVo commonVo = new CommonVo();
            commonVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            responseVo.add(commonVo);
            settleVo.setTransactions(responseVo);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, settleVo);

        }

        return responseVo;
    }

    private void doValidation(SettleTransactionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        ValidationUtils.isEquals("Settle", dto.getMethod(), InvalidRequestException::new);

        if (dto.getMode().equals(GameType.BETNSETTLE.code)) {
            ValidationUtils.validateRequest(dto.getBet());
        }

        if (!vendorService.isValidDateTime(dto.actionDate)) {
            throw new InvalidRequestException();
        }
    }

    private void doVerification(SettleTransactionDto dto, GameSession gameSession) throws
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
        if (dto.getMode().equals(GameType.BETNSETTLE.code)) {
            //bet settle type record
            validationService.validateEligibleBet(gameSession, dto.getLoginId());
        }

    }

    private CommonVo processData(SettleTransactionDto settleTransactionDto, HttpServletRequest request) {
        CommonVo commonVo = new CommonVo();
        GameSession gameSession = null;

        //Generate new traceId
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        //Insert Request Body
        Gson gson = new Gson();
        httpRequestLog.setRequestBody(gson.toJson(settleTransactionDto));

        try {

            // 1. Validate each user data
            this.doValidation(settleTransactionDto);

            // 2. Verify session token
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(settleTransactionDto.getLoginId().toLowerCase(), settleTransactionDto.getGameName().toLowerCase().replaceAll("\\s", ""));

            // 3. Verify Credential and Currency
            this.doVerification(settleTransactionDto, gameSession);

            // 4. Process Result
            Boolean isBet = true;
            if (settleTransactionDto.getMode().equals(GameType.SETTLE.code)) {
                isBet = false;
            }
            ResultType resultType = vendorService.calculateResultType(settleTransactionDto.getBetAmount(), settleTransactionDto.getWinAmount(), settleTransactionDto.getJackpotAmount(), isBet);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleTransactionDto, resultType, vendorService, httpRequestLog);

            // 5. Set respond Vo
            commonVo.getDataVo().setBalance(balance.setScale(2, RoundingMode.DOWN));

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

        } catch (BetNotFoundException betNotFoundException) {
            commonVo.setResponseCode(ResponseCodes.RECORD_NOT_FOUND);
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            commonVo.setResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, insufficientBalanceException);

        } catch (TransactionStillProcessingException | MergedBetDataIntegrityException systemErrorException) {
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
