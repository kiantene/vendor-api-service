package com.nextgen.gameaggregator.vendor.aasexy.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aasexy.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import com.nextgen.gameaggregator.vendor.aasexy.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.aasexy.vo.GeneralVo;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class BetService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final ValidationService validationService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public BetService(GameSessionService gameSessionService,
                      WalletService walletService,
                      HttpService httpService,
                      VendorService vendorService,
                      ValidationService validationService,
                      BetActionLogService betActionLogService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.validationService = validationService;
        this.betActionLogService = betActionLogService;
    }

    public BetVo bet(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        BetVo vo = new BetVo();
        RequestDto<BetDto> dto = null;
        GameSession gameSession = null;
        boolean processFailed = false;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_SIZE);
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();

            // Decode the URL-encoded message
            String decodedBody = URLDecoder.decode(body, StandardCharsets.UTF_8);

            // Convert JsonNode back to JSON string
            String convertedJsonString = vendorService.convertBodyToJson(decodedBody);
            dto = HttpService.convertJsonToDto(convertedJsonString, new TypeReference<>() {
            });

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get and check first transaction username and game code for game session
            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getMessage().getTxns().get(0).getUserId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getMessage().getTxns().get(0).getGameCode(), gameSession);

            // Verify first transaction and game session
            this.doVerification(dto.getMessage().getTxns().get(0), gameSession);

            // Process all transaction with completable future
            List<CompletableFuture<BalanceVo>> balanceVoList = new LinkedList<>();
            for (BetTransactionsDto transaction : dto.getMessage().getTxns()) {
                GameSession finalGameSession = gameSession;
                CompletableFuture<BalanceVo> balanceVo = CompletableFuture.supplyAsync(() -> processData(transaction, body, request, finalGameSession), executor);
                balanceVoList.add(balanceVo);
            }

            // Get latest balance, if have fail response then return error
            BigDecimal balance = vendorService.checkResponseAndReturnBalance(balanceVoList);
            if (balance == null) {
                processFailed = true;
                throw new BetFailedException("Have Transaction Failed");
            }

            vo.setBalance(balance.setScale(3, RoundingMode.DOWN));
            vo.setBalanceTs(vendorService.convertDateTimeFormat(System.currentTimeMillis()));

        } catch (AuthenticationException e){
            vo.setResponseCodes(ResponseCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            vo.setResponseCodes(ResponseCodes.INSUFFICIENT_BALANCE);
            httpService.logError(httpRequestLog, e);
        } catch (GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_GAME);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException | InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PARAMETERS);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.FAIL);
            vo.setHttpStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
            httpService.logError(httpRequestLog, e);
        } finally {
            // close executor
            executor.shutdown();
            // Failed then rollback all transaction
            if (processFailed) {
                //insert to collection for rollback all bet
                this.prepareRollback(dto.getMessage().getTxns(), gameSession);
            }
        }
        return vo;
    }

    private void doValidation(RequestDto<BetDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Check first transaction game code
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getGameCode())){
            throw new InvalidRequestException();
        }
        // Check first transaction user id
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getUserId())){
            throw new InvalidRequestException();
        }
    }

    private void doValidation(BetTransactionsDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetTransactionsDto betTransactionsDto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException, CurrencyNotSupportedException, GameNotSupportedException {
        ///validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, betTransactionsDto.getUserId());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betTransactionsDto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(betTransactionsDto.getGameId()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), betTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private void doBetVerification(BetTransactionsDto betTransactionsDto, GameSession gameSession) throws DuplicateExternalTransactionIdException {
        // Verify bet have rollback before
        vendorService.verifyBetAfterRollback(gameSession.getVendorPlayerId(), betTransactionsDto.getExternalTransactionId());
    }

    private BalanceVo processData(BetTransactionsDto betTransactionsDto, String body, HttpServletRequest request, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(betTransactionsDto));
        String traceId = httpRequestLog.getId();
        BalanceVo balanceVo = null;
        try {
            // Validate each user data
            this.doValidation(betTransactionsDto);

            // Verify session token
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betTransactionsDto.getGameCode(), gameSession);

            // Verify data
            this.doBetVerification(betTransactionsDto, gameSession);

            // Process Result
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betTransactionsDto, body, httpRequestLog);
            balanceVo = new BalanceVo(betEvent.getLastBalance(), httpRequestLog.getOperatorTimestamp());

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            balanceVo = new BalanceVo(e.getBalance(), httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (DuplicateExternalTransactionIdException e) {
            BigDecimal balance = vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog);
            balanceVo = new BalanceVo(balance, httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            balanceVo = new BalanceVo(BigDecimal.ONE.negate(), httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // do nothing, return null
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balanceVo;
    }

    private void prepareRollback(List<BetTransactionsDto> transactionList, GameSession gameSession) {
        THREAD_POOL.submit(() -> {
            for (BetTransactionsDto transaction : transactionList) {
                GeneralRollbackDto generalRollbackDto = new GeneralRollbackDto();
                generalRollbackDto.setRollbackId(transaction.getExternalTransactionId());
                generalRollbackDto.setVendorSettledTime(null);
                generalRollbackDto.setRoundId(transaction.getRoundId());
                betActionLogService.create(new Gson().toJson(generalRollbackDto), transaction.getRoundId(), transaction.getVendorBetId(), transaction.getExternalTransactionId(), gameSession, 1, null);
            }
        });
    }
}
