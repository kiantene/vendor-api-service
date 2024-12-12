package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotency;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.*;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
public class CreditAction {

    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final UnsettledBetService unsettledBetService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;
    private final VendorGameService vendorGameService;

    public CreditAction(
            HttpService httpService,
            VendorLineService vendorLineService,
            GameSessionService gameSessionService,
            UnsettledBetService unsettledBetService,
            WalletService walletService,
            VendorService vendorService,
            WalletRequestService walletRequestService,
            OperatorWalletService operatorWalletService,
            VendorGameService vendorGameService) {

        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.unsettledBetService = unsettledBetService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
        this.vendorGameService = vendorGameService;
    }

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

            // 2. Validate and Verified each UserDto inside balanceDto
            List<TransactionsVo> transactionsList = new ArrayList<>();
            for (CreditTransactionsDto transaction : creditDto.getTransactions()) {
                TransactionsVo transactionsVo = processData(transaction, clientId, clientSecret, request);
                transactionsList.add(transactionsVo);
            }
            creditVo.setTransactions(transactionsList);

        } catch (InvalidRequestException e) {
            creditVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Request");
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException e) {
            creditVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Invalid Body Format");
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            creditVo.setResponseCode(ResponseCodes.SYSTEM_ERROR);
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

    private void doVerification(CreditTransactionsDto creditTransactionsDto, GameSession gameSession, String clientId, String clientSecret)
            throws
            CredentialNotFoundException,
            InvalidVendorLineException,
            CurrencyNotSupportedException,
            GameNotSupportedException, InvalidRequestException, TransactionStillProcessingException, BetNotFoundException {

        // 1. Validate Credentials
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        Optional.ofNullable(CLIENT_ID).orElseThrow(CredentialNotFoundException::new);
        Optional.ofNullable(CLIENT_SECRET).orElseThrow(CredentialNotFoundException::new);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 2. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = VendorService.splitGameCode(gameSession.getVendorGameCode(), 2);
        String gpcode = parts[0];
        String gamecode = parts[1];
        ValidationUtils.isEquals(creditTransactionsDto.getGpcode(), gpcode, GameNotSupportedException::new);
        ValidationUtils.isEquals(creditTransactionsDto.getGamecode(), gamecode, GameNotSupportedException::new);
        ValidationUtils.isEquals(creditTransactionsDto.getCur(), gameSession.getVendorCurrencyCode(), CurrencyNotSupportedException::new);

        // 3. Validate TxType is exist
        if (!Txtype.txtTypeList.contains(creditTransactionsDto.getTxtype())) {
            throw new InvalidRequestException();
        }

        // 4. Validate Debit Transaction is exist, except txtype 590 (End Round)
        if (!creditTransactionsDto.getTxtype().equals(Txtype.END_ROUND)) {
            vendorService.verifyExistDebitTransaction(gameSession.getVendorId(), gameSession.getVendorPlayerId(), creditTransactionsDto.getRefptxid());
        }
    }

    private void dataMapper(WalletRequest walletRequest, CreditTransactionsDto dto, GameSession gameSession) {
        walletRequestService.updateByGameSession(walletRequest, gameSession);
        walletRequest.setVendorPlayerUsername(dto.getUserid());
        walletRequest.setExternalTransactionId(dto.getExternalTransactionId());
        walletRequest.setRoundId(dto.getRoundId());
        walletRequest.setVendorGameCode(dto.getGamecode());
        walletRequest.setTimestamp(dto.getVendorBetTime());
        walletRequest.setToken(gameSession.getToken());
        walletRequest.setVendorBetId(dto.getVendorBetId());
        walletRequest.setTransferAmount(dto.getAmt());
        walletRequest.setBetAmount(dto.getBetAmount());
        walletRequest.setWinAmount(dto.getWinAmount());
        walletRequest.setEffectiveTurnover(dto.getEffectiveTurnover());
        walletRequest.setJackpotAmount(BigDecimal.ZERO);
        walletRequest.setResultType(ResultType.BET_WIN.code);
        walletRequest.setVendorBetTime(dto.getVendorBetTime());
        walletRequest.setVendorSettleTime(dto.getVendorSettleTime());
    }

    private TransactionsVo processData(CreditTransactionsDto creditTransactionsDto, String clientId, String clientSecret, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
        String traceId = httpRequestLog.getId();
        TransactionsVo transactionsVo = new TransactionsVo();
        GameSession gameSession = null;
        boolean requireDebit = false;

        try {
            // 1. Validate each user data
            this.doValidation(creditTransactionsDto);

            // 2. Verify session token
            String vendorGameCode = VendorService.mergeGameCode(creditTransactionsDto.getGpcode(), creditTransactionsDto.getGamecode());
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(creditTransactionsDto.getUserid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(vendorGameCode, gameSession);
            VendorGame vendorGame = vendorGameService.getByVendorGameId(gameSession.getVendorGameId());
            requireDebit = vendorGame.getRequireDebit();

            // 3. Verify Credential and Currency
            this.doVerification(creditTransactionsDto, gameSession, clientId, clientSecret);

            // 4. Process Result / Rollback
            BigDecimal balance;
            if (creditTransactionsDto.getTxtype().equals(Txtype.CANCEL_BET)) {
                RollbackTransactionDto rollbackTransactionDto = new ModelMapper().map(creditTransactionsDto, RollbackTransactionDto.class);
                balance = walletService.processRollback(traceId, rollbackTransactionDto, gameSession, vendorService, httpRequestLog);
            } else if (creditTransactionsDto.getTxtype().equals(Txtype.END_ROUND)) {
                List<UnsettledBet> unsettledBet = unsettledBetService.getByRoundId(creditTransactionsDto.getExternalroundid(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());
                if (unsettledBet.isEmpty()) {
                    balance = this.getBalance(traceId, gameSession);
                } else {
                    if (!requireDebit) {
                        ResultType resultType = vendorService.calculateResultType(creditTransactionsDto.getBetAmount(), creditTransactionsDto.getWinAmount(), creditTransactionsDto.getJackpotAmount(), false, creditTransactionsDto.getBetStatus());
                        balance = walletService.processBetResult(traceId, gameSession, creditTransactionsDto, resultType, vendorService, httpRequestLog);
                    } else {
                        this.checkForDuplicateRequest(creditTransactionsDto);
                        this.dataMapper(walletRequest, creditTransactionsDto, gameSession);
                        walletRequest = operatorWalletService.betCredit(walletRequest);
                        balance = walletRequest.getBalanceAfter();
                    }
                }
            } else {
                if (!requireDebit) {
                    ResultType resultType = vendorService.calculateResultType(creditTransactionsDto.getBetAmount(), creditTransactionsDto.getWinAmount(), creditTransactionsDto.getJackpotAmount(), false, creditTransactionsDto.getBetStatus());
                    balance = walletService.processBetResult(traceId, gameSession, creditTransactionsDto, resultType, vendorService, httpRequestLog);
                } else {
                    this.checkForDuplicateRequest(creditTransactionsDto);
                    this.dataMapper(walletRequest, creditTransactionsDto, gameSession);
                    walletRequest = operatorWalletService.betCredit(walletRequest);
                    balance = walletRequest.getBalanceAfter();
                }
            }
            // 5. Set transactionsVo
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(creditTransactionsDto.getPtxid());
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
        } catch (GameNotSupportedException e) {
            transactionsVo.setResponseCode(ResponseCodes.INVALID_ARGUMENTS, "Invalid Game Code");
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
            transactionsVo.setPtxid(creditTransactionsDto.getPtxid());
            transactionsVo.setDup(true);
        } catch (TransactionStillProcessingException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Transaction Still Processing");
            httpService.logError(httpRequestLog, e);
        } catch (InvalidOperatorResponseException e) {
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Processing Error");
            httpService.logError(httpRequestLog, e);
        } catch (BetNotFoundException e) {
            transactionsVo.setTxid(traceId);
            transactionsVo.setPtxid(creditTransactionsDto.getPtxid());
            transactionsVo.setResponseCode(ResponseCodes.SYSTEM_ERROR, "Bet Not Found");
            if (creditTransactionsDto.getTxtype().equals(Txtype.CANCEL_BET)) {
                transactionsVo.setResponseCode(ResponseCodes.TRANSACTION_DOES_NOT_EXIST);
            }
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

    private void checkForDuplicateRequest(CreditTransactionsDto creditTransactionsDto) throws DuplicateRequestException {
        RequestIdempotency requestIdempotency = new RequestIdempotency() {
            @Override
            public String getTransactionId() {
                return creditTransactionsDto.getVendorBetId();
            }

            @Override
            public String getVendorPlayerUsername() {
                return creditTransactionsDto.getUserid();
            }
        };

        httpService.isDuplicateRequest(requestIdempotency);
    }
}
