package com.nextgen.gameaggregator.vendor.aasexy.api.tips;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.service.BetActionLogService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexy.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aasexy.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.aasexy.service.VendorService;
import com.nextgen.gameaggregator.vendor.aasexy.vo.BalanceVo;
import com.nextgen.gameaggregator.vendor.aasexy.vo.GeneralVo;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
public class TipsService {
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;

    @Autowired
    public TipsService(GameSessionService gameSessionService,
                       WalletService walletService,
                       HttpService httpService,
                       VendorService vendorService,
                       BetActionLogService betActionLogService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
    }

    public TipsVo tips(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        TipsVo vo = new TipsVo();
        GameSession gameSession = null;
        RequestDto<TipsDto> dto = null;
        boolean processFailed = false;

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
            for (TipsTransactionsDto transaction : dto.getMessage().getTxns()) {
                GameSession finalGameSession = gameSession;
                CompletableFuture<BalanceVo> balanceVo = CompletableFuture.supplyAsync(() -> processData(transaction, request, finalGameSession));
                balanceVoList.add(balanceVo);
            }

            // Get latest balance, if have fail response then return error
            BigDecimal balance = vendorService.checkResponseAndReturnBalance(balanceVoList);
            if(balance == null){
                processFailed = true;
                throw new BetFailedException("Have Transaction Failed");
            }

            vo.setBalance(balance.setScale(3, RoundingMode.DOWN));
            vo.setBalanceTs(vendorService.convertDateTimeFormat(System.currentTimeMillis()));

        } catch (AuthenticationException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_TOKEN);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException | InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_PARAMETERS);
        } catch (GameNotSupportedException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_GAME);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            vo.setResponseCodes(ResponseCodes.FAIL);
            httpService.logError(httpRequestLog, e);
        } finally {
            // Failed then rollback all transaction
            if(processFailed){
                //insert to collection
                this.prepareRollback(dto.getMessage().getTxns(), gameSession);
            }
        }
        return vo;
    }

    private void doValidation(RequestDto<TipsDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getGameCode())){
            throw new InvalidRequestException();
        }
        if(StringUtils.isBlank(dto.getMessage().getTxns().get(0).getUserId())){
            throw new InvalidRequestException();
        }
    }

    private void doValidation(TipsTransactionsDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(TipsTransactionsDto tipsTransactionsDto, GameSession gameSession) throws InvalidPlayerException, CurrencyNotSupportedException, GameNotSupportedException {
        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), tipsTransactionsDto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify valid game id
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(tipsTransactionsDto.getGameId()), GameNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), tipsTransactionsDto.getUserId(), InvalidPlayerException::new);
    }

    private void doTipVerification(TipsTransactionsDto tipsTransactionsDto, GameSession gameSession) throws DuplicateExternalTransactionIdException {
        // Verify bet have rollback before
        vendorService.verifyBetAfterRollback(gameSession.getVendorPlayerId(), tipsTransactionsDto.getExternalTransactionId());
    }

    private BalanceVo processData(TipsTransactionsDto tipsTransactionsDto, HttpServletRequest request, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(new Gson().toJson(tipsTransactionsDto));
        String traceId = httpRequestLog.getId();
        BalanceVo balanceVo = null;
        try {
            // 1. Validate each user data
            this.doValidation(tipsTransactionsDto);

            // 2. Verify session token
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(tipsTransactionsDto.getGameCode(), gameSession);

            this.doTipVerification(tipsTransactionsDto, gameSession);

            // Process Result
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, tipsTransactionsDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
            balanceVo = new BalanceVo(balance, httpRequestLog.getOperatorTimestamp());

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e){
            balanceVo = new BalanceVo(e.getBalance(), httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (DuplicateExternalTransactionIdException e) {
            BigDecimal balance = vendorService.getCurrentBalance(traceId, gameSession, httpRequestLog);
            balanceVo = new BalanceVo(balance, httpRequestLog.getOperatorTimestamp());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e){
            // do nothing
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, new GeneralVo());
        }
        return balanceVo;
    }

    private void prepareRollback(List<TipsTransactionsDto> transactionList, GameSession gameSession){
        THREAD_POOL.submit(() -> {
            for (TipsTransactionsDto transaction : transactionList) {
                GeneralRollbackDto generalRollbackDto = new GeneralRollbackDto();
                generalRollbackDto.setRollbackId(transaction.getExternalTransactionId());
                generalRollbackDto.setVendorSettledTime(null);
                generalRollbackDto.setRoundId(transaction.getRoundId());
                betActionLogService.create(new Gson().toJson(generalRollbackDto), transaction.getRoundId(), transaction.getVendorBetId(),transaction.getExternalTransactionId(),gameSession, 1, null);
            }
        });
    }
}
