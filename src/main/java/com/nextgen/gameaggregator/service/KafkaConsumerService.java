package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.Features;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.betResult.WalletBetResultAction;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackAction;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralRollbackDto;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralSettleDto;
import com.nextgen.gameaggregator.service.data.VendorFeatureDataService;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.BetHistoryPublishContext;
import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.api.cancelbet.CancelBetDto;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
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
    private final AgentApiVersionService agentApiVersionService;
    private final Set<Integer> skipVendorList;
    private final UnsettledBetCachingService unsettledBetCachingService;
    private final WalletService walletService;
    private final GameSessionService gameSessionService;
    private final BetHistoryProducer betHistoryProducer;
    private final VendorFeatureDataService vendorFeatureService;

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
                                AgentApiVersionService agentApiVersionService,
                                UnsettledBetCachingService unsettledBetCachingService,
                                WalletService walletService,
                                GameSessionService gameSessionService,
                                BetHistoryProducer betHistoryProducer,
                                VendorFeatureDataService vendorFeatureService) {

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
        this.agentApiVersionService = agentApiVersionService;
        this.unsettledBetCachingService = unsettledBetCachingService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.betHistoryProducer = betHistoryProducer;
        this.vendorFeatureService = vendorFeatureService;
        this.skipVendorList = new HashSet<>(Set.of(2, 7)); //PGSOFT, SPADEGAMING
    }

    private void toSendOrNotToSend(String traceId, AgentPlayer agentPlayer, VendorPlayer vendorPlayer, GameSession gameSession, SettledBet settledBet, VendorCurrency vendorCurrency, EndRoundSettledBet endRoundSettledBet, HttpRequestLog httpRequestLog, String operatorResultType) {

        Integer agentApiVersion = agentApiVersionService.getAgentApiVersion(agentPlayer.getAgentId());

        if (operatorResultType == null || operatorResultType.isEmpty()) {
            operatorResultType.equals("END");
        }

        if (agentApiVersion == 2 && this.skipVendorList.contains(vendorPlayer.getVendorId())) {
            // Skip notifyEndRoundProcess for version 2
            httpRequestLog.setUrl(httpRequestLog.getUrl() + " (SKIP PROCESS END ROUND FOR VERSION 2)");
        } else {
            this.notifyEndRoundProcess(traceId, agentPlayer, vendorPlayer, gameSession, settledBet,
                    vendorCurrency.getFromVendorRate(), vendorCurrency.getToVendorRate(),
                    endRoundSettledBet, settledBet, httpRequestLog, operatorResultType);
        }
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_END_ROUND_PROCESS_V3, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
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
            if (vendorFeatureService.isVendorEnabled(Features.AGENT_MAX_PAYOUT, settledBet.getVendorId())) {
                /**
                 * Feature toggle to use refactored logic
                 */
                doPublishBetHistory(
                        settledBet,
                        vendorService,
                        gameSession,
                        agentPlayer.getUsername(),
                        vendorPlayer.getUsername(),
                        vendorCurrency.getFromVendorRate()
                );
            } else {
                BetHistory betHistory = new BetHistory(settledBet);
                if (!vendorService.getBetPreprocess().getIsPreProcessBet()) {
                    // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
                    // kafkaService.produceWarehouseBetHistory
                    //         (betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
                    kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(),
                            gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());
                } else {
                    // process bet as preprocessing bet and send to kafka topic_bet_history_preprocessing topic
                    kafkaService.producePreprocessingBetHistory(betHistory, agentPlayer.getUsername(), vendorPlayer.getUsername(), vendorCurrency.getFromVendorRate());
                }
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

    private void doPublishBetHistory(SettledBet settledBet,
                                     BaseVendorService vendorService,
                                     GameSession gameSession,
                                     String agentPlayerUsername,
                                     String vendorPlayerUsername,
                                     BigDecimal fromVendorConversionRate) {
        boolean requirePreprocessing = vendorService.getBetPreprocess().getIsPreProcessBet();
        BetHistoryPublishContext publishContext = new BetHistoryPublishContext(
                gameSession.getProductCode(),
                gameSession.getProductId(),
                gameSession.getProductGameId(),
                agentPlayerUsername,
                vendorPlayerUsername,
                fromVendorConversionRate,
                requirePreprocessing,
                null
        );
        betHistoryProducer.publish(settledBet, publishContext);
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


    @KafkaListener(topics = KafkaConstant.TOPIC_REFUND_PROCESS, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeRefundProcess(String message) {

        //prepare endRoundProcess Log
        Exception exception = null;
        ObjectMapper objectMapper = new ObjectMapper();
        EndRoundSettledBetForPatching refundedSettledBetForPatching = null;
        String newTraceId = UUID.randomUUID().toString();
        HttpRequestLog httpRequestLog = httpService.startRefundConsumerLog();

        ProcessEndRoundLog processEndRoundLog = new ProcessEndRoundLog();
        processEndRoundLog.setStartTime(System.currentTimeMillis());
        processEndRoundLog.setTraceId(newTraceId);

        try {
            //prepare endRound and settleBet info
            refundedSettledBetForPatching = objectMapper.readValue(message, EndRoundSettledBetForPatching.class);
            refundedSettledBetForPatching.setOperatorStatus(ResponseCodes.Status.SC_OK.code);
            SettledBet settledBet = new SettledBet(refundedSettledBetForPatching);
            settledBet.setResultType(refundedSettledBetForPatching.getGaResultType());

            //get unsettled bet
            UnsettledBet unsettledBet = unsettledBetService.getUnsettledBetByRoundId(settledBet.getVendorBetId(), settledBet.getRoundId(), settledBet.getVendorGameId(), settledBet.getVendorPlayerId());
            settledBet.setStatus(BetStatus.REFUNDED.code);

            processEndRoundLog.setRawBody(refundedSettledBetForPatching.getRawData());
            processEndRoundLog.setRoundId(settledBet.getRoundId());
            processEndRoundLog.setVendorBetId(settledBet.getVendorBetId());

            httpRequestLog.setBetStart(System.currentTimeMillis());

            AgentPlayer agentPlayer = agentPlayerService.get(refundedSettledBetForPatching.getAgentPlayerId());
            VendorPlayer vendorPlayer = vendorPlayerService.getByVendorPlayerId(refundedSettledBetForPatching.getVendorPlayerId(), null);

            //refund, no need verify pre processing vendor game

            //get vendorCurrencyRate for the vendor
            GameSession gameSession = new GameSession(refundedSettledBetForPatching);
            VendorCurrency vendorCurrency = vendorService.getCurrencyConversionRate(gameSession, newTraceId);

            //update settledBet info
            settledBetService.save(settledBet, settledBet.getRawData());

            //prepare insert new betHistory data
            BetHistory betHistory = new BetHistory(settledBet);

            // process bet as normal bet and send to kafka topic_warehouse_bet_history topic
            // kafkaService.produceWarehouseBetHistory
            kafkaService.produceBetHistoryV3(betHistory, gameSession.getProductCode(), gameSession.getProductId(), gameSession.getProductGameId(), gameSession.getAgentPlayerUsername(), gameSession.getVendorPlayerUsername(), vendorCurrency.getFromVendorRate());

            //prepare delete unsettledBet
            unsettledBetService.delete(unsettledBet);

            //prepare and send endRound to operator
            if (refundedSettledBetForPatching.getSendToOperator() == 1) {
                this.notifyRollbackProcess(newTraceId, agentPlayer, vendorPlayer, gameSession, vendorCurrency.getToVendorRate(), refundedSettledBetForPatching, settledBet, httpRequestLog);

            } else {
                GeneralVo vo = new GeneralVo();
                vo.setResponseCode(ResponseCode.SUCCESS);

                httpRequestLog.setId(newTraceId);
                httpRequestLog.setRoundId(settledBet.getRoundId());
                httpRequestLog.setRequestBody(refundedSettledBetForPatching.getRawData());
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

        } catch (Exception e) {
            exception = e;

        } finally {
            if (exception != null) {
                //prepare and save processEndRoundLog if exception not null;
                processEndRoundLog.setEndTime(System.currentTimeMillis());
                RequestService.processEndRoundLogPatching(processEndRoundLog, exception, refundedSettledBetForPatching);
            }
        }
    }

    private void notifyRollbackProcess(String traceId, AgentPlayer agentPlayer, VendorPlayer vendorPlayer, GameSession gameSession, BigDecimal toVendorConversionRate, EndRoundSettledBet endRoundSettledBet, SettledBet settledBet, HttpRequestLog httpRequestLog) {
        THREAD_POOL.submit(() -> {
            Exception exception = null;
            GeneralVo vo = new GeneralVo();
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);

            try {
                httpRequestLog.setId(traceId);
                httpRequestLog.setRoundId(settledBet.getRoundId());
                httpRequestLog.setRequestBody(endRoundSettledBet.getRawData());
                httpRequestLog.setAgentId(settledBet.getAgentId());
                httpRequestLog.setVendorBetId(settledBet.getVendorBetId());
                httpRequestLog.setVendorUsername(vendorPlayer.getUsername());
                httpRequestLog.setOperatorUsername(agentPlayer.getUsername());
                httpRequestLog.setVendorId(settledBet.getVendorId());
                httpRequestLog.setRequestType(WalletRollbackAction.class.getSimpleName());
                httpRequestLog.setGameToken(settledBet.getGameSessionToken());

                walletRollbackAction.callProcessRollback(traceId, agentPlayer.getAgentId(), gameSession, settledBet.getBetId(), settledBet.getRoundId(), settledBet.getVendorBetId(), settledBet.getVendorSettleTime(), settledBet.getInternalTransactionId(), httpRequestLog, 5000, toVendorConversionRate);
                vo.setResponseCode(ResponseCode.SUCCESS);

            } catch (InvalidAgentApiCredentialException e) {
                httpService.logError(httpRequestLog, e);
                exception = e;

            } catch (Exception e) {
                httpService.logError(httpRequestLog, e);
                exception = e;

            } finally {
                httpRequestLog.setBetEnd(System.currentTimeMillis());
                httpRequestLog.setBetTimeTaken(httpRequestLog.getBetEnd() - httpRequestLog.getBetStart());
                httpService.end(httpRequestLog, vo);

            }
        });
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_RECON_FOR_UNSETTLED_BET, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void consumeReconDataPatching(String message) {
        HttpRequestLog log = httpService.startReconDataPatchingLog();
        String traceId = log.getId();
        GeneralVo vo = new GeneralVo();

        try {
            // set request body to show original kafka data
            log.setRequestBody(message);
            // convert to Object
            EndRoundSettledBetForPatching endRoundSettledBetForPatching = parseMessage(message);
            // validate vendorPlayerId or vendorPlayerUsername
            validateBetData(endRoundSettledBetForPatching);
            // generate gameSession by vendorPlayerId or vendorPlayerUsername
            GameSession session = initGameSession(endRoundSettledBetForPatching);

            // process Rollback or BetResult
            if (endRoundSettledBetForPatching.isRefund()) {
                processRollbackCase(endRoundSettledBetForPatching, session, log);
            } else {
                processSettleCase(traceId, endRoundSettledBetForPatching, session, log);
            }

            vo.setResponseCode(ResponseCode.SUCCESS);
        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(log, e);
        } finally {
            httpService.end(log, vo);
        }
    }

    @KafkaListener(topics = KafkaConstant.TOPIC_PATCHING_SPORT_UNSETTLED_BET_TO_REFUND_BET, groupId = KafkaConstant.GROUP_ID, containerFactory = "customKafkaListenerContainerFactory")
    public void patchingSportUnsettledBetToRefundBet(String message) {
        HttpRequestLog log = httpService.startReconDataPatchingLog();
        String traceId = log.getId();
        GeneralVo vo = new GeneralVo();

        try {
            // set request body to show original kafka data
            log.setRequestBody(message);
            // convert to Object
            EndRoundSettledBetForPatching endRoundSettledBetForPatching = parseMessage(message);
            // validate vendorPlayerId or vendorPlayerUsername
            validateBetData(endRoundSettledBetForPatching);
            // generate gameSession by vendorPlayerId or vendorPlayerUsername
            GameSession session = initGameSession(endRoundSettledBetForPatching);

            // process Rollback or BetResult
            if (endRoundSettledBetForPatching.isRefund()) {
                processRollbackCase(endRoundSettledBetForPatching, session, log);
            } else {
                processSettleCase(traceId, endRoundSettledBetForPatching, session, log);
            }

            vo.setResponseCode(ResponseCode.SUCCESS);
        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(log, e);
        } finally {
            httpService.end(log, vo);
        }
    }

    private EndRoundSettledBetForPatching parseMessage(String message) throws IOException {
        return new ObjectMapper().readValue(message, EndRoundSettledBetForPatching.class);
    }

    private void validateBetData(EndRoundSettledBetForPatching betData) throws InvalidRequestException {
        String username = betData.getVendorPlayerUsername();
        Long vendorPlayerId = betData.getVendorPlayerId();
        if (vendorPlayerId == null && (username == null || username.isBlank())) {
            throw new InvalidRequestException();
        }
    }

    private GameSession initGameSession(EndRoundSettledBetForPatching betData) throws InvalidPlayerException {
        Long vendorPlayerId = betData.getVendorPlayerId();
        String username = betData.getVendorPlayerUsername();
        return (vendorPlayerId != null)
                ? gameSessionService.generateNewSessionTokenByVendorPlayerId(vendorPlayerId)
                : gameSessionService.generateNewSessionToken(username);
    }

    private void processRollbackCase(EndRoundSettledBetForPatching betData, GameSession session, HttpRequestLog log) throws BetNotFoundException, InvalidAgentApiCredentialException, RecordNotFoundException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, BetRefundIdempotentViolationException, TransactionStillProcessingException, InvalidOperatorResponseException, InvalidFormatException, GameNotSupportedException {
        UnsettledBet unsettledBet = unsettledBetService.getByVendorIdAndExternalTransactionId(
                session.getVendorId(),
                betData.getExternalTransactionId()
        );

        gameSessionService.updateByVendorCurrencyId(session);
        gameSessionService.updateByVendorGameId(session, unsettledBet.getVendorGameId());
        session.setToken(unsettledBet.getGameSessionToken());
        session.setVendorToken(unsettledBet.getGameSessionToken());

        GeneralRollbackDto dto = new GeneralRollbackDto();
        dto.setRoundId(unsettledBet.getRoundId());
        dto.setRollbackId(unsettledBet.getExternalTransactionId());
        dto.setVendorSettledTime(betData.getVendorSettleTime());

        walletService.processRollback(dto, session, new GeneralVendorService(), log);
    }

    private void processSettleCase(String traceId, EndRoundSettledBetForPatching betData, GameSession session, HttpRequestLog log) throws InvalidAgentApiCredentialException, VendorCurrencyNotSupportException, BetResultIdempotentViolationException, MergedBetDataIntegrityException, InsufficientBalanceException, TransactionStillProcessingException, BetNotFoundException, InvalidOperatorResponseException, InternalServerTimeoutRetryException, GameNotSupportedException {
        List<UnsettledBet> unsettledBetList = unsettledBetCachingService
                .getByRoundId(betData.getRoundId())
                .stream()
                .filter(bet -> Objects.equals(bet.getVendorPlayerId(), session.getVendorPlayerId()))
                .toList();

        if (unsettledBetList.isEmpty()) throw new BetNotFoundException();

        BigDecimal totalBetAmount = calculateTotalBetAmount(unsettledBetList);

        gameSessionService.updateByVendorCurrencyId(session);
        gameSessionService.updateByVendorGameId(session, unsettledBetList.get(0).getVendorGameId());
        session.setToken(unsettledBetList.get(0).getGameSessionToken());
        session.setVendorToken(unsettledBetList.get(0).getGameSessionToken());

        ResultType resultType = determineResultType(totalBetAmount, betData.getBetAmount());

        GeneralSettleDto dto = generateGeneralSettleDto(betData, totalBetAmount, unsettledBetList.get(0).getVendorBetTime());
        walletService.processBetResult(traceId, session, dto, resultType, new GeneralVendorService(), log);
    }

    private BigDecimal calculateTotalBetAmount(List<UnsettledBet> unsettledBetList) {
        return unsettledBetList.stream()
                .map(UnsettledBet::getBetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ResultType determineResultType(BigDecimal totalBet, BigDecimal expectedBet) {
        return (totalBet.compareTo(expectedBet) != 0) ? ResultType.BET_WIN : ResultType.WIN;
    }

    private GeneralSettleDto generateGeneralSettleDto(EndRoundSettledBetForPatching endRoundSettledBetForPatching, BigDecimal totalBetAmount, Long vendorBetTime) {

        BigDecimal betAmount = endRoundSettledBetForPatching.getBetAmount();
        if (totalBetAmount.compareTo(betAmount) > 0) {
            betAmount = betAmount.subtract(totalBetAmount);
        } else {
            betAmount = BigDecimal.ZERO;
        }

        GeneralSettleDto dto = new GeneralSettleDto();
        dto.setExternalTransactionId(endRoundSettledBetForPatching.getExternalTransactionId());
        dto.setVendorBetId(endRoundSettledBetForPatching.getVendorBetId());
        dto.setRoundId(endRoundSettledBetForPatching.getRoundId());
        dto.setBetAmount(betAmount);
        dto.setWinAmount(endRoundSettledBetForPatching.getWinAmount());
        dto.setWinLoss(endRoundSettledBetForPatching.getWinLoss());
        dto.setEffectiveTurnover(endRoundSettledBetForPatching.getEffectiveTurnover() != null ? endRoundSettledBetForPatching.getEffectiveTurnover() : null);
        dto.setVendorBetTime(endRoundSettledBetForPatching.getVendorBetTime() != null ? endRoundSettledBetForPatching.getVendorBetTime() : vendorBetTime);
        dto.setVendorSettleTime(endRoundSettledBetForPatching.getVendorSettleTime());
        dto.setResultTime(endRoundSettledBetForPatching.getResultTime());
        dto.setBetStatus(BetStatus.SETTLED);
        return dto;
    }
}