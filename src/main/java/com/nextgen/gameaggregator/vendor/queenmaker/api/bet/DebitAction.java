package com.nextgen.gameaggregator.vendor.queenmaker.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.*;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
public class DebitAction {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;
    private final VendorGameService vendorGameService;

    public DebitAction(
            HttpService httpService,
            VendorLineService vendorLineService,
            GameSessionService gameSessionService,
            WalletService walletService,
            ValidationService validationService,
            VendorService vendorService,
            WalletRequestService walletRequestService,
            OperatorWalletService operatorWalletService,
            VendorGameService vendorGameService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
        this.vendorGameService = vendorGameService;
    }

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

            String body = httpRequestLog.getRequestBody();
            DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(debitDto);

            // 2. Validate and Verified each UserDto inside balanceDto
            List<TransactionsVo> transactionsList = new ArrayList<>();
            for (DebitTransactionsDto transaction : debitDto.getTransactions()) {
                TransactionsVo transactionsVo = processData(transaction, clientId, clientSecret, body, request);
                transactionsList.add(transactionsVo);
            }
            debitVo.setTransactions(transactionsList);

        } catch (InvalidRequestException e) {
            debitVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Request");
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException e) {
            debitVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Body Format");
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            debitVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, debitVo);
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
            AuthenticationException, BetResultIdempotentViolationException {

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
        String[] parts = VendorService.splitGameCode(gameSession.getVendorGameCode(), 2);
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(debitTransactionsDto.getGpcode(), gpcode, GameNotSupportedException::new);
        ValidationUtils.isEquals(debitTransactionsDto.getGamecode(), gamecode, GameNotSupportedException::new);
        ValidationUtils.isEquals(debitTransactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);

        // 4. Validate TxType is exist
        if (!Txtype.txtTypeList.contains(debitTransactionsDto.getTxtype())) {
            throw new InvalidRequestException();
        }

        // 5. Check Transaction is settled
        vendorService.checkBetIsSettled(gameSession, debitTransactionsDto);
    }

    private void dataMapper(WalletRequest walletRequest, DebitTransactionsDto dto, GameSession gameSession) {
        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(dto.getUserid());
        walletRequest.setExternalTransactionId(dto.getExternalTransactionId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(dto.getGamecode());
        walletRequest.setTimestamp(dto.getVendorBetTime());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getVendorBetId());
        walletRequest.setTransferAmount(dto.getAmt());
    }

    private TransactionsVo processData(DebitTransactionsDto debitTransactionsDto, String clientId, String clientSecret, String body, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        String traceId = httpRequestLog.getId();
        TransactionsVo transactionsVo = new TransactionsVo();
        GameSession gameSession = null;
        boolean requireDebit = false;

        try {
            // 1. Validate each user data
            this.doValidation(debitTransactionsDto);

            // 2. Verify session token
            String vendorGameCode = VendorService.mergeGameCode(debitTransactionsDto.getGpcode(), debitTransactionsDto.getGamecode());
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(debitTransactionsDto.getUserid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(vendorGameCode, gameSession);
            VendorGame vendorGame = vendorGameService.getByVendorGameId(gameSession.getVendorGameId());
            requireDebit = vendorGame.getRequireDebit();

            // 3. Verify Credential and Currency
            this.doVerification(debitTransactionsDto, gameSession, clientId, clientSecret);

            // 4. Process bet
            if (!requireDebit) {
                BetEvent betEvent = walletService.processBet(traceId, gameSession, debitTransactionsDto, body, httpRequestLog);
                transactionsVo.setBal(betEvent.getLastBalance());
            } else {
                this.checkForDuplicateRequest(debitTransactionsDto);
                this.dataMapper(walletRequest, debitTransactionsDto, gameSession);
                walletRequest = operatorWalletService.betDebit(walletRequest);
                transactionsVo.setBal(walletRequest.getBalanceAfter());
            }

            // 5. Set transactionsVo
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(debitTransactionsDto.getPtxid());
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
        } catch (GameNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Game Code");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException e) {
            transactionsVo.setResponseCode(ResponseCodes.INCORRECT_FORMAT);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 InvalidVendorLineException |
                 InvalidAgentApiCredentialException |
                 CredentialNotFoundException e) {
            transactionsVo.setResponseCode(ResponseCodes.OPERATION_FAILED_DETERMINISTICALLY);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledAgentPlayerException e) {
            transactionsVo.setResponseCode(ResponseCodes.USER_BLOCKED);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidPlayerException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Player");
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            // return current balance
            transactionsVo.setBal(this.getBalance(traceId, gameSession));
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(debitTransactionsDto.getPtxid());
            transactionsVo.setDup(true);
        } catch (TransactionStillProcessingException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Transaction Still Processing");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Processing Error");
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            if (!requireDebit) {
                httpService.end(httpRequestLog, transactionsVo);
            } else {
                walletRequestService.end(walletRequest, httpRequestLog, transactionsVo);
            }
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

    private void checkForDuplicateRequest(DebitTransactionsDto debitTransactionsDto) throws DuplicateRequestException {
        RequestIdempotency requestIdempotency = new RequestIdempotency() {
            @Override
            public String getTransactionId() {
                return debitTransactionsDto.getVendorBetId();
            }

            @Override
            public String getVendorPlayerUsername() {
                return debitTransactionsDto.getUserid();
            }
        };

        httpService.isDuplicateRequest(requestIdempotency);
    }
}

