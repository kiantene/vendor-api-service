package com.nextgen.gameaggregator.service;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetActionLog;
import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.repository.ga.writer.RawBetActionLogRepository;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralAdjustmentDto;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralSettleDto;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class BetActionLogService {
    public static final Integer MAX_RETRY_COUNTER = 7;
    private static final Map<Integer, Long> TIME_INTERVAL = Map.of(
            1, 30000L, // 30 seconds
            2, 60000L, // 1 minute
            3, 180000L, // 3 minutes
            4, 720000L, // 12 minutes
            5, 3600000L, // 1 hour
            6, 21600000L, // 6 hours
            7, 21600000L // 6 hours
    );
    private final RawBetActionLogRepository rawBetActionLogRepository;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final WalletAdjustmentService walletAdjustmentService;

    @Autowired
    public BetActionLogService(RawBetActionLogRepository rawBetActionLogRepository,
                               GameSessionService gameSessionService, WalletService walletService, AutowireCapableBeanFactory autowireCapableBeanFactory, HttpService httpService, VendorService vendorService, WalletAdjustmentService walletAdjustmentService) {
        this.rawBetActionLogRepository = rawBetActionLogRepository;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.autowireCapableBeanFactory = autowireCapableBeanFactory;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.walletAdjustmentService = walletAdjustmentService;
    }

    public void create(String processData, String roundId, String vendorBetId, String externalTransactionId, GameSession gameSession, Integer action, ResultType resultType) {

        RawBetActionLog rawBetActionLog = new RawBetActionLog();
        Long nextRetryTime = System.currentTimeMillis();
        Integer defaultRetryCounter = 1;

        rawBetActionLog.setId(externalTransactionId + "_" + action + "_" + gameSession.getVendorPlayerUsername() + "_" + gameSession.getVendorGameCode());
        rawBetActionLog.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        rawBetActionLog.setVendorBetId(vendorBetId);
        rawBetActionLog.setRoundId(roundId);
        rawBetActionLog.setExternalTransactionId(externalTransactionId);
        rawBetActionLog.setToken(gameSession.getToken());
        rawBetActionLog.setProcessData(processData);
        rawBetActionLog.setNextRetryTime(this.calculateNextRetryTime(defaultRetryCounter, nextRetryTime));
        rawBetActionLog.setAction(action);
        rawBetActionLog.setStatus(1);
        rawBetActionLog.setRetryCounter(defaultRetryCounter);
        rawBetActionLog.setCreateDate(System.currentTimeMillis());
        rawBetActionLog.setResultType(resultType);
        rawBetActionLogRepository.save(rawBetActionLog);

    }

    public Long calculateNextRetryTime(Integer retryCounter, Long nextRetryTime) {

        //change into a fix time interval
        nextRetryTime = nextRetryTime + TIME_INTERVAL.getOrDefault(retryCounter, 720000L);

        return nextRetryTime;
    }

    public void asyncProcessRetryRequestByList(List<RawBetActionLog> rawBetActionLogList, Long currentTime) {
        List<CompletableFuture<Void>> futures = rawBetActionLogList.stream()
                .map(rawBetActionLogListItem -> processAction(rawBetActionLogListItem, currentTime))
                .toList();

        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join(); // Ensure we wait for all futures to complete
    }

    public CompletableFuture<Void> processAction(RawBetActionLog betActionLog, Long currentTime) {
        return CompletableFuture.runAsync(() -> {
            rawBetActionLogRepository.delete(betActionLog);
            if (betActionLog.getAction() == 1) {
                this.processRollback(betActionLog, currentTime);
            } else if (betActionLog.getAction() == 2) {
                this.processBetResult(betActionLog,currentTime);
            } else {
                this.processAdjustment(betActionLog,currentTime);
            }
        });
    }

    public void processRollback(RawBetActionLog betActionLog, Long currentTime) {
        GameSession gameSession;
        HttpRequestLog httpRequestLog = httpService.startBetActionRequest(betActionLog);
        httpRequestLog.setRequestBody(new Gson().toJson(betActionLog));

        try {
            GeneralRollbackDto generalRollbackDto = new Gson().fromJson(betActionLog.getProcessData(), GeneralRollbackDto.class);

            try {
                gameSession = gameSessionService.verifyToken(betActionLog.getToken());
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(betActionLog.getVendorPlayerUsername());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(betActionLog.getToken());
                gameSession.setVendorToken(betActionLog.getToken());
            }
            Vendor vendor = vendorService.getById(gameSession.getVendorId());
            BaseVendorService baseVendorService = (BaseVendorService) autowireCapableBeanFactory.getBean(vendor.getClassName() + "VendorService");

            walletService.processRollback(generalRollbackDto, gameSession, baseVendorService, httpRequestLog);
        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            betActionLog.setRetryCounter(betActionLog.getRetryCounter() + 1);
            betActionLog.setNextRetryTime(this.calculateNextRetryTime(betActionLog.getRetryCounter(), currentTime));
            if (betActionLog.getRetryCounter().equals(MAX_RETRY_COUNTER)) {
                betActionLog.setStatus(0);
            } else {
                //will only save back once is failed, and not hitting maxRetryCounter
                rawBetActionLogRepository.save(betActionLog);
            }
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog,new GeneralVo());
        }
    }

    public void processBetResult(RawBetActionLog betActionLog, Long currentTime) {
        GameSession gameSession;
        HttpRequestLog httpRequestLog = httpService.startBetActionRequest(betActionLog);
        httpRequestLog.setRequestBody(new Gson().toJson(betActionLog));
        String traceId = httpRequestLog.getId();

        try {
            GeneralSettleDto generalSettleDto = new Gson().fromJson(betActionLog.getProcessData(), GeneralSettleDto.class);

            try {
                gameSession = gameSessionService.verifyToken(betActionLog.getToken());
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(betActionLog.getVendorPlayerUsername());
                gameSessionService.updateByVendorGameCode(gameSession, generalSettleDto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(betActionLog.getToken());
                gameSession.setVendorToken(betActionLog.getToken());
            }
            Vendor vendor = vendorService.getById(gameSession.getVendorId());
            BaseVendorService baseVendorService = (BaseVendorService) autowireCapableBeanFactory.getBean(vendor.getClassName() + "VendorService");

            walletService.processBetResult(traceId, gameSession, generalSettleDto, betActionLog.getResultType(), baseVendorService, httpRequestLog);
        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            betActionLog.setRetryCounter(betActionLog.getRetryCounter() + 1);
            betActionLog.setNextRetryTime(this.calculateNextRetryTime(betActionLog.getRetryCounter(), currentTime));
            if (betActionLog.getRetryCounter().equals(MAX_RETRY_COUNTER)) {
                betActionLog.setStatus(0);
            } else {
                //will only save back once is failed, and not hitting maxRetryCounter
                rawBetActionLogRepository.save(betActionLog);
            }
            httpService.logError(httpRequestLog, e);
        } finally {
            //testing
            httpService.end(httpRequestLog,new GeneralVo());
        }
    }

    public void processAdjustment(RawBetActionLog betActionLog, Long currentTime) {
        GameSession gameSession;
        HttpRequestLog httpRequestLog = httpService.startBetActionRequest(betActionLog);
        httpRequestLog.setRequestBody(new Gson().toJson(betActionLog));
        httpRequestLog.setVendorUsername("");
        String traceId = httpRequestLog.getId();

        try {
            GeneralAdjustmentDto generalAdjustmentDto = new Gson().fromJson(betActionLog.getProcessData(), GeneralAdjustmentDto.class);

            try {
                gameSession = gameSessionService.verifyToken(betActionLog.getToken());
            } catch (AuthenticationException e) {
                gameSession = gameSessionService.generateNewSessionToken(betActionLog.getVendorPlayerUsername());
                gameSessionService.updateByVendorGameCode(gameSession, generalAdjustmentDto.getGameId());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(betActionLog.getToken());
                gameSession.setVendorToken(betActionLog.getToken());
            }

            walletAdjustmentService.processAdjustment(traceId, gameSession, generalAdjustmentDto, httpRequestLog);
        } catch (Exception e) {
            betActionLog.setRetryCounter(betActionLog.getRetryCounter() + 1);
            betActionLog.setNextRetryTime(this.calculateNextRetryTime(betActionLog.getRetryCounter(), currentTime));
            if (betActionLog.getRetryCounter().equals(MAX_RETRY_COUNTER)) {
                betActionLog.setStatus(0);
            } else {
                //will only save back once is failed, and not hitting maxRetryCounter
                rawBetActionLogRepository.save(betActionLog);
            }
            httpService.logError(httpRequestLog, e);
        } finally {
            //testing
            httpService.end(httpRequestLog,new GeneralVo());
        }
    }
}
