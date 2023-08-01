package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction {
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

    @PostMapping(path = EndPoints.WALLET_CREDIT)
    public CreditVo CreditAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        CreditVo creditVo = new CreditVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);

            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(creditDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<CompletableFuture<TransactionsVo>> futures = new LinkedList<>();
            for (CreditTransactionsDto transaction : creditDto.getTransactions()) {
                String traceId = httpRequestLog.getId();
                CompletableFuture<TransactionsVo> future = CompletableFuture.supplyAsync(() -> processData(transaction, clientId, clientSecret, traceId, httpRequestLog));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<TransactionsVo> transactionsList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            creditVo.setTransactions(transactionsList);


        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, creditVo);
        }

        return creditVo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CreditTransactionsDto transactionsDto, GameSession gameSession, String clientId, String clientSecret)
            throws
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException {

        // 1. Validate Credentials
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        Optional.ofNullable(CLIENT_ID).orElseThrow(CredentialNotFoundException::new);
        Optional.ofNullable(CLIENT_SECRET).orElseThrow(CredentialNotFoundException::new);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 2. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = VendorService.splitGameCode(gameSession.getVendorGameCode());
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(transactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);

        ValidationUtils.isEquals(transactionsDto.getGpcode(), gpcode, InvalidVendorLineException::new);
        ValidationUtils.isEquals(transactionsDto.getGamecode(), gamecode, GameNotSupportedException::new);

    }

    private TransactionsVo processData(CreditTransactionsDto creditTransactionsDto, String clientId, String clientSecret, String traceId, HttpRequestLog httpRequestLog) {
        TransactionsVo transactionsVo = new TransactionsVo();

        try {
            // 1. Validate each user data
            this.doValidation(creditTransactionsDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(creditTransactionsDto.getUserid(), creditTransactionsDto.getGamecode());

            // 3. Verify Credential and Currency
            this.doVerification(creditTransactionsDto, gameSession, clientId, clientSecret);

            // 4. Process Result
            ResultType resultType = vendorService.calculateResultType(creditTransactionsDto.getBetAmount(), creditTransactionsDto.getWinAmount(), creditTransactionsDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditTransactionsDto, resultType, vendorService, httpRequestLog);

            // 5. Set transactionsVo
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(traceId);
            transactionsVo.setBal(balance);
            transactionsVo.setCur(gameSession.getVendorCurrencyCode());

        } catch (AuthenticationException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_OR_EXPIRED_TOKEN);

        } catch (CurrencyNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.CURRENCY_MISMATCH);

        } catch (InsufficientBalanceException e) {
            transactionsVo.setResponseCode(ResponseCodes.INSUFFICIENT_FUNDS);

        } catch (GameNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Game Code");

        } catch (InvalidRequestException e) {
            transactionsVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT, e.getValidation().values().iterator().next().toString());

        } catch (InvalidVendorLineException |
                 CredentialNotFoundException |
                 InvalidAgentApiCredentialException e) {
            transactionsVo.setResponseCode(ResponseCodes.OPERATION_FAILED_DETERMINISTICALLY);

        } catch (BetResultIdempotentViolationException e) {
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(traceId);
            transactionsVo.setDup(true);

        } catch (TransactionStillProcessingException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Transaction Still Processing");

        } catch (InvalidOperatorResponseException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Processing Error");

        } catch (BetNotFoundException e) {
            transactionsVo.setResponseCode(ResponseCodes.TRANSACTION_DOES_NOT_EXIST);

        } catch (MergedBetDataIntegrityException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, transactionsVo);
        }

        return transactionsVo;
    }

}
