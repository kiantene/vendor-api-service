package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackAction;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.api.cancelbet.CancelBetDto;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class KafkaConsumerService {
    private static final Integer THREAD_SIZE = 64;
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final WalletBetResultAction walletBetResultAction;
    private final WalletRollbackAction walletRollbackAction;
    private final SettledBetService settledBetService;
    private final KafkaService kafkaService;
    private final UnsettledBetService unsettledBetService;
    private final VendorService vendorService;
    private final SportWalletService sportWalletService;
    private final HttpService httpService;
    private final AgentPlayerService agentPlayerService;
    private final VendorPlayerService vendorPlayerService;
    private final CachingService cachingService;
    private final AgentApiVersionService agentApiVersionService;
    private final Set<Integer> skipVendorList;
    private final UnsettledBetCachingService unsettledBetCachingService;

    public KafkaConsumerService(WalletBetResultAction walletBetResultAction,
                                WalletRollbackAction walletRollbackAction,
                                SettledBetService settledBetService,
                                KafkaService kafkaService,
                                UnsettledBetService unsettledBetService,
                                VendorService vendorService,
                                SportWalletService sportWalletService,
                                HttpService httpService,
                                AgentPlayerService agentPlayerService,
                                VendorPlayerService vendorPlayerService,
                                CachingService cachingService,
                                AgentApiVersionService agentApiVersionService,
                                UnsettledBetCachingService unsettledBetCachingService) {
        
        this.walletBetResultAction = walletBetResultAction;
        this.walletRollbackAction = walletRollbackAction;
        this.settledBetService = settledBetService;
        this.kafkaService = kafkaService;
        this.unsettledBetService = unsettledBetService;
        this.vendorService = vendorService;
        this.sportWalletService = sportWalletService;
        this.httpService = httpService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.cachingService = cachingService;
        this.agentApiVersionService = agentApiVersionService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.skipVendorList = new HashSet<>(Set.of(2, 7)); //PGSOFT, SPADEGAMING
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS_V2, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcessV2(String message) throws RecordNotFoundException, InvalidPlayerException, BetNotFoundException {

        //prepare endRoundProcess Log
        Exception exception = null;
        String newTraceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startEndRoundConsumerLog();

        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setTraceId(newTraceId);

        //prepare endRound and settleBet info
        EndRoundSettledBet endRoundSettledBet = new Gson().fromJson(message, EndRoundSettledBet.class);
        endRoundSettledBet.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
        SettledBet settledBet = new SettledBet(endRoundSettledBet);
        settledBet.setResultType(endRoundSettledBet.getGaResultType());

        //check if unsettledBet still exist, if no longer exist, throw BetNotFoundException
        UnsettledBet unsettledBet = unsettledBetService.getUnsettledBetByRoundId(settledBet.getVendorBetId(), settledBet.getRoundId(), settledBet.getVendorGameId(), settledBet.getVendorPlayerId());

        processEndRoundLog.setRawBody(endRoundSettledBet.getRawData());
        processEndRoundLog.setRoundId(settledBet.getRoundId());
        processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());

        try {
            httpRequestLog.setBetStart(System.currentTimeMillis());

            AgentPlayer agentPlayer = agentPlayerService.get(endRoundSettledBet.getAgentPlayerId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(endRoundSettledBet.getVendorPlayerId(), null);

            //get is bet = sidebet
            vendorService.verifyIsPreProcessingVendorGame(endRoundSettledBet.getVendorGameId());

            //get vendorCurrencyRate for the vendor
            GameSession gameSession = new GameSession(endRoundSettledBet);
            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

            //update settledBet info
            settledBetService.save(settledBet, settledBet.getRawData());

            //prepare insert new betHistory data
            BetHistory betHistory = new BetHistory(settledBet);
            if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                // process bet as normal bet and send to kafka topic_bet_history topic
                kafkaService.produceBetHistory(betHistory, gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());
                // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                // kafkaService.produceWarehouseBetHistory
                //         (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
                kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                        gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername());
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            unsettledBetService.delete(unsettledBet);

            this.toSendOrNotToSend(newTraceId, agentPlayer, vendorPlayer, gameSession, settledBet, vendorCurrency, endRoundSettledBet, httpRequestLog, "END");

        } catch (GameNotSupportedException e) {
            exception = e;

        } catch (VendorCurrencyNotSupportException e) {
            exception = e;

        } catch (Exception e) {
            exception = e;

        } finally {
            if (exception != null) {
                //prepare and save processEndRoundLog if exception not null;
                processEndRoundLog.setEndTime(System.currentTimeMillis());
                RequestService.processEndRoundLog(processEndRoundLog, exception, endRoundSettledBet);
            }
        }
    }

    private void toSendOrNotToSend(String traceId, AgentPlayer agentPlayer, VendorPlayer vendorPlayer, GameSession gameSession, SettledBet settledBet, VendorCurrency vendorCurrency, EndRoundSettledBet endRoundSettledBet, HttpRequestLog httpRequestLog, String operatorResultType) {

        Integer agentApiVersion = agentApiVersionService.getAgentApiVersion(agentPlayer.getAgentId());

        if (agentApiVersion == 2 && this.skipVendorList.contains(vendorPlayer.getVendorId())) {
            // Skip notifyEndRoundProcess for version 2
            httpRequestLog.setUrl(httpRequestLog.getUrl() + " (SKIP PROCESS END ROUND FOR VERSION 2)");
        } else {
            this.notifyEndRoundProcess(traceId, agentPlayer, vendorPlayer, gameSession, settledBet,
                    vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate(),
                    endRoundSettledBet, settledBet, httpRequestLog, operatorResultType);
        }
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS_V3, groupId = KafkaConstant.GROUP_ID + "33312", containerFactory = "customKafkaListenerContainerFactory")
    public void consumeEndRoundProcessV3(String message) {

        //prepare endRoundProcess Log
        Exception exception = null;
        ObjectMapper objectMapper = new ObjectMapper();
        EndRoundSettledBetForPatching endRoundSettledBetForPatching = null;
        String newTraceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startEndRoundConsumerLog();

        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setTraceId(newTraceId);

        try {
            //prepare endRound and settleBet info
            endRoundSettledBetForPatching = objectMapper.readValue(message, EndRoundSettledBetForPatching.class);
            endRoundSettledBetForPatching.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            SettledBet settledBet = new SettledBet(endRoundSettledBetForPatching);
            settledBet.setResultType(endRoundSettledBetForPatching.getGaResultType());

            //TODO TO BE REVISITED FOR FUTURE ENHANCEMENT TO EASE RECON TEAM WORK
            //For WIN, will require to get unsettledBet.betId to overwrite for settledBet
            //For BET_WIN, will not require because it will consider as new transaction.
            //For END, might require to get from unsettledBet too, TO BE REVISITED.
//            if (endRoundSettledBetForPatching.getOperatorResultType().equals("WIN")) {
//                this.processResultTypeWin(settledBet);
//            }

            processEndRoundLog.setRawBody(endRoundSettledBetForPatching.getRawData());
            processEndRoundLog.setRoundId(settledBet.getRoundId());
            processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());

            httpRequestLog.setBetStart(System.currentTimeMillis());

            AgentPlayer agentPlayer = agentPlayerService.get(endRoundSettledBetForPatching.getAgentPlayerId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(endRoundSettledBetForPatching.getVendorPlayerId(), null);

            //get is bet = sidebet
            vendorService.verifyIsPreProcessingVendorGame(endRoundSettledBetForPatching.getVendorGameId());

            //get vendorCurrencyRate for the vendor
            GameSession gameSession = new GameSession(endRoundSettledBetForPatching);
            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

            //update settledBet info
            settledBetService.save(settledBet, settledBet.getRawData());

            //prepare insert new betHistory data
            BetHistory betHistory = new BetHistory(settledBet);
            if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                // process bet as normal bet and send to kafka topic_bet_history topic
                kafkaService.produceBetHistory(betHistory, gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());
                // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                // kafkaService.produceWarehouseBetHistory
                //         (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
                kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                        gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername());
            } else {
                // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                kafkaService.producePreprocessingBetHistory(betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
            }

            //prepare delete unsettledBet
            UnsettledBet unsettledBet = new UnsettledBet(settledBet);
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator

            if (endRoundSettledBetForPatching.getSendToOperator() == 1) {
                this.toSendOrNotToSend(newTraceId, agentPlayer, vendorPlayer, gameSession, settledBet, vendorCurrency, endRoundSettledBetForPatching, httpRequestLog, endRoundSettledBetForPatching.getOperatorResultType());

            } else {
                GeneralVo vo = new GeneralVo();
                vo.setResponseCode(ResponseCode.SUCCESS);

                httpRequestLog.setId(newTraceId);
                httpRequestLog.setRoundId(settledBet.getRoundId());
                httpRequestLog.setRequestBody(endRoundSettledBetForPatching.getRawData());
                httpRequestLog.setAgentId(settledBet.getAgentId());
                httpRequestLog.setVendorBetId(settledBet.getVendorBetId());
                httpRequestLog.setVendorUsername(vendorPlayer.getUsername());
                httpRequestLog.setOperatorUsername(agentPlayer.getUsername());
                httpRequestLog.setVendorId(settledBet.getVendorId());
                httpRequestLog.setRequestType(WalletBetResultAction.class.getSimpleName());
                httpRequestLog.setGameToken(settledBet.getGameSessionToken());
                httpRequestLog.setBetEnd(System.currentTimeMillis());
                httpRequestLog.setBetTimeTaken(httpRequestLog.getBetEnd() - httpRequestLog.getBetStart());
                httpRequestLog.setOperatorStart(0L);
                httpRequestLog.setOperatorEnd(0L);
                httpService.end(httpRequestLog, vo);
            }

        } catch (GameNotSupportedException e) {
            exception = e;

        } catch (VendorCurrencyNotSupportException e) {
            exception = e;

        } catch (Exception e) {
            exception = e;

        } finally {
            if (exception != null) {
                //prepare and save processEndRoundLog if exception not null;
                processEndRoundLog.setEndTime(System.currentTimeMillis());
                RequestService.processEndRoundLogPatching(processEndRoundLog, exception, endRoundSettledBetForPatching);
            }
        }
    }

    private void processResultTypeWin(SettledBet settledBet) {

        List<UnsettledBet> unsettledBetList = unsettledBetCachingService.getByRoundId(settledBet.getRoundId());

        UnsettledBet unsettledBet = unsettledBetList.stream()
                .filter(unsettledBetData -> settledBet.getVendorBetId().equals(unsettledBetData.getVendorBetId()))
                .findFirst()
                .orElse(null);

        settledBet.setBetId((unsettledBet != null) ? unsettledBet.getBetId() : settledBet.getBetId());

    }

    private void notifyEndRoundProcess(String traceId, AgentPlayer agentPlayer, VendorPlayer vendorPlayer, GameSession gameSession, BetInformation betInformation, BigDecimal fromVendorConversionRate, BigDecimal toVendorConversionRate, EndRoundSettledBet endRoundSettledBet, SettledBet settledBet, HttpRequestLog httpRequestLog, String operatorResultType) {
        THREAD_POOL.submit(() -> {
            Exception exception = null;
            GeneralVo vo = new GeneralVo();
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);

            try {
                ResultType resultType = ResultType.getResultTypeByDescription(operatorResultType);
                httpRequestLog.setId(traceId);
                httpRequestLog.setRoundId(settledBet.getRoundId());
                httpRequestLog.setRequestBody(endRoundSettledBet.getRawData());
                httpRequestLog.setAgentId(settledBet.getAgentId());
                httpRequestLog.setVendorBetId(settledBet.getVendorBetId());
                httpRequestLog.setVendorUsername(vendorPlayer.getUsername());
                httpRequestLog.setOperatorUsername(agentPlayer.getUsername());
                httpRequestLog.setVendorId(settledBet.getVendorId());
                httpRequestLog.setRequestType(WalletBetResultAction.class.getSimpleName());
                httpRequestLog.setGameToken(settledBet.getGameSessionToken());
                walletBetResultAction.callProcessEndRound(traceId, agentPlayer.getAgentId(), gameSession, betInformation, resultType, fromVendorConversionRate, toVendorConversionRate, httpRequestLog);
                vo.setResponseCode(ResponseCode.SUCCESS);

            } catch (InvalidAgentApiCredentialException e) {
                httpService.logError(httpRequestLog, e);
                exception = e;

            } catch (Exception e) {
                httpService.logError(httpRequestLog, e);
                exception = e;

            } finally {
                httpRequestLog.setBetEnd(System.currentTimeMillis());
                httpService.end(httpRequestLog, vo);

            }
        });
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_RAW_SETTLED_BET, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRawSettledBet(String message) {
        String traceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startInternalConsumerForRawSettledBet();
        GeneralVo vo = new GeneralVo();

        try {
            SportRawSettledBet sportRawSettledBet = new Gson().fromJson(message, SportRawSettledBet.class);
            BetEvent responseVo = null;

            //to handle saba send results in bulk consists of refund bet.
            if (sportRawSettledBet.getBetStatus().equals(BetStatus.REFUNDED)) {
                CancelBetDto cancelBetDto = new CancelBetDto();
                cancelBetDto.setRefId(sportRawSettledBet.getRoundId());
                cancelBetDto.setOperationId(sportRawSettledBet.getExternalTransactionId());
                cancelBetDto.setUserId(sportRawSettledBet.getVendorPlayerUsername());
                responseVo = sportWalletService.refund(traceId, cancelBetDto, httpRequestLog);
                responseVo.setLastBalance(responseVo == null ? BigDecimal.ZERO : responseVo.getLastBalance());

            } else {
                responseVo = sportWalletService.settle(traceId, sportRawSettledBet, httpRequestLog);
            }

            vo.setBalance(responseVo.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            e.printStackTrace();

        } finally {
            httpService.end(httpRequestLog, vo);

        }
    }
}