package com.nextgen.gameaggregator.vendor.queenmaker.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.*;
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
public class DebitAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.WALLET_DEBIT)
    public DebitVo DebitAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        DebitVo debitVo = new DebitVo();

        try {
            // Retrieve request body in original string format and convert into dto
            String clientId = request.getHeader(Formats.HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(Formats.HEADER_CLIENT_SECRET);
            Optional.ofNullable(clientId).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(clientSecret).orElseThrow(InvalidRequestException::new);

            String traceId = httpRequestLog.getId();
            String body = httpRequestLog.getRequestBody();
            DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(debitDto);

            // 2. Validate and Verified each UserDto inside balanceDto using Asynchronous
            List<CompletableFuture<TransactionsVo>> futures = new LinkedList<>();
            for (DebitTransactionsDto transaction : debitDto.getTransactions()) {

                CompletableFuture<TransactionsVo> future = CompletableFuture.supplyAsync(() -> processData(transaction, clientId, clientSecret, traceId, body, request));
                futures.add(future);
            }
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
            allFutures.join();
            List<TransactionsVo> transactionsList = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            debitVo.setTransactions(transactionsList);

        } catch (InvalidRequestException e) {
            debitVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Request");

        } catch (JsonProcessingException e) {
            debitVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Body Format");

        } catch (Exception e) {
            debitVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, debitVo);
            log.info("QM Debit Request Log : " + httpRequestLog.getRequestBody());
        }

        return debitVo;
    }

    private <T> void doValidation(T dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(DebitTransactionsDto debitTransactionsDto, GameSession gameSession, String clientId, String clientSecret)
            throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException,
            AuthenticationException {

        //1. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, debitTransactionsDto.getUserid());

        // 2. Validate Credentials
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        Optional.ofNullable(CLIENT_ID).orElseThrow(CredentialNotFoundException::new);
        Optional.ofNullable(CLIENT_SECRET).orElseThrow(CredentialNotFoundException::new);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 3. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = vendorService.splitGameCode(gameSession.getVendorGameCode(), 2);
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(debitTransactionsDto.getGpcode(), gpcode, GameNotSupportedException::new);
        ValidationUtils.isEquals(debitTransactionsDto.getGamecode(), gamecode, GameNotSupportedException::new);
        ValidationUtils.isEquals(debitTransactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);

        // 4. Validate TxType is exist
        if (!Txtype.txtTypeList.contains(debitTransactionsDto.getTxtype())) {
            throw new InvalidRequestException();
        }
    }

    private TransactionsVo processData(DebitTransactionsDto debitTransactionsDto, String clientId, String clientSecret, String traceId, String body, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        TransactionsVo transactionsVo = new TransactionsVo();
        GameSession gameSession = null;

        try {
            // 1. Validate each user data
            this.doValidation(debitTransactionsDto);

            // 2. Verify session token
            String vendorGameCode = vendorService.mergeGameCode(debitTransactionsDto.getGpcode(), debitTransactionsDto.getGamecode());
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(debitTransactionsDto.getUserid(), vendorGameCode);

            // 3. Verify Credential and Currency
            this.doVerification(debitTransactionsDto, gameSession, clientId, clientSecret);

            // 4. Process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitTransactionsDto, body, httpRequestLog);

            // 5. Set transactionsVo
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(debitTransactionsDto.getPtxid());
            transactionsVo.setBal(betEvent.getLastBalance());
            transactionsVo.setCur(gameSession.getVendorCurrencyCode());
            transactionsVo.setDup(false);

        } catch (AuthenticationException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_OR_EXPIRED_TOKEN);

        } catch (CurrencyNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.CURRENCY_MISMATCH);

        } catch (InsufficientBalanceException e) {
            transactionsVo.setResponseCode(ResponseCodes.INSUFFICIENT_FUNDS);

        } catch (GameNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Game Code");

        } catch (InvalidRequestException e) {
            transactionsVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 InvalidVendorLineException |
                 InvalidAgentApiCredentialException |
                 CredentialNotFoundException e) {
            transactionsVo.setResponseCode(ResponseCodes.OPERATION_FAILED_DETERMINISTICALLY);

        } catch (DisabledAgentPlayerException e) {
            transactionsVo.setResponseCode(ResponseCodes.USER_BLOCKED);

        } catch (InvalidPlayerException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Player");

        } catch (BetResultIdempotentViolationException e) {
            transactionsVo.setBal(this.getBalance(traceId, gameSession));
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(debitTransactionsDto.getPtxid());
            transactionsVo.setDup(true);

        } catch (TransactionStillProcessingException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Transaction Still Processing");

        } catch (InvalidOperatorResponseException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Processing Error");

        } catch (Exception e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, transactionsVo);
            log.info("QM Debit Request Log : " + httpRequestLog);
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

