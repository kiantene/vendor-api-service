package com.nextgen.gameaggregator.vendor.queenmaker.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.*;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import com.nextgen.gameaggregator.vendor.queenmaker.vo.TransactionsVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    @Autowired
    private UnsettledBetService unsettledBetService;
    @Autowired
    private RedissonService redissonService;

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
            GameNotSupportedException, InvalidRequestException, TransactionStillProcessingException, BetNotFoundException, InvalidFormatException {

        // 1. Validate Credentials
        String CLIENT_ID = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_ID);
        String CLIENT_SECRET = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CLIENT_SECRET);
        Optional.ofNullable(CLIENT_ID).orElseThrow(CredentialNotFoundException::new);
        Optional.ofNullable(CLIENT_SECRET).orElseThrow(CredentialNotFoundException::new);
        ValidationUtils.isEquals(clientId, CLIENT_ID, InvalidVendorLineException::new);
        ValidationUtils.isEquals(clientSecret, CLIENT_SECRET, InvalidVendorLineException::new);

        // 2. Validate Vendor Currency Code, Brand Code, Game Code
        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = vendorService.splitGameCode(gameSession.getVendorGameCode(), 2);
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

    private TransactionsVo processData(CreditTransactionsDto creditTransactionsDto, String clientId, String clientSecret, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        TransactionsVo transactionsVo = new TransactionsVo();
        GameSession gameSession = null;

        try {
            // 1. Validate each user data
            this.doValidation(creditTransactionsDto);

            // 2. Verify session token
            String vendorGameCode = vendorService.mergeGameCode(creditTransactionsDto.getGpcode(), creditTransactionsDto.getGamecode());
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(creditTransactionsDto.getUserid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(vendorGameCode, gameSession);

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
                    ResultType resultType = vendorService.calculateResultType(creditTransactionsDto.getBetAmount(), creditTransactionsDto.getWinAmount(), creditTransactionsDto.getJackpotAmount(), false, creditTransactionsDto.getBetStatus());
                    balance = walletService.processBetResult(traceId, gameSession, creditTransactionsDto, resultType, vendorService, httpRequestLog);
                }
            } else {
                ResultType resultType = vendorService.calculateResultType(creditTransactionsDto.getBetAmount(), creditTransactionsDto.getWinAmount(), creditTransactionsDto.getJackpotAmount(), false, creditTransactionsDto.getBetStatus());
                balance = walletService.processBetResult(traceId, gameSession, creditTransactionsDto, resultType, vendorService, httpRequestLog);
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
            if(creditTransactionsDto.getTxtype().equals(Txtype.CANCEL_BET)){
                transactionsVo.setResponseCode(ResponseCodes.TRANSACTION_DOES_NOT_EXIST);
            }
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
