package com.nextgen.gameaggregator.core.engine.game.round;

import com.nextgen.gameaggregator.constant.RedisKeyConstant;
import com.nextgen.gameaggregator.constant.WalletServiceConstant;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextHolder;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.entity.ga.EndRoundSettledBet;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.service.AgentApiVersionService;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.LoggingService;
import com.nextgen.gameaggregator.service.UnsettledBetService;
import com.nextgen.gameaggregator.service.data.producer.GameRoundProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoundService {
    private final AgentApiVersionService agentApiVersionService;
    private final EndRoundProcessor endRoundProcessor;
    private final UnsettledBetService unsettledBetService;
    private final LoggingService loggingService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GameRoundProducer gameRoundProducer;

    // to exclude vendors not using unsettled bets (eg. PGSoft)
    private final List<Integer> asyncEndRoundVendorExclusionList = List.of(
            2 // PGSoft
    );

    @Value("${endround-process.retry-interval-in-seconds:5}")
    private long retryIntervalInSecondsValue;

    /**
     * Handles a round-ended signal and publishes a RoundEnded notification only when allowed.
     */
    public void publishRoundEnded(String roundId, GameSession gameSession) {
        if (!isRoundEndedPublishAllowed(gameSession)) {
            return;
        }

        SettleType settleType = BetResultContextHolder.getConfig().getSettleType();
        GameRoundEndedEvent event = GameRoundEndedEvent.ofGameSession(gameSession, roundId);
        gameRoundProducer.publishRoundEnded(event, settleType.name());

        BetResultContextHolder.clear();
    }

    private boolean isRoundEndedPublishAllowed(GameSession gameSession) {
        if (!BetResultContextHolder.isInitialized()) {
            return false;
        }

        SettleType settleType = BetResultContextHolder.getConfig().getSettleType();
        if (settleType == null) {
            return false;
        }

        // only api version 3 will run below logic
        Integer agentApiVersion = agentApiVersionService.getAgentApiVersion(gameSession.getAgentId());
        return agentApiVersion != null && agentApiVersion == 3;
    }

    public void notifyEndRoundAsync(SettledBet settledBet, BaseVendorService vendorService, GameSession gameSession, String traceId) {
        String settledBetRoundId = settledBet.getRoundId();
        Integer settledBetVendorId = settledBet.getVendorId();

        if (asyncEndRoundVendorExclusionList.contains(gameSession.getVendorId())) {
            return;
        }

        loggingService.logDataFlowByVendor("Inside notifyEndRoundAsync 1", settledBetVendorId, settledBetRoundId, settledBet);
        taskScheduler.schedule(() -> {
            try {
                loggingService.logDataFlowByVendor("Inside notifyEndRoundAsync 2", settledBetVendorId, settledBetRoundId, settledBet);
                String roundId = settledBet.getRoundId();
                Integer vendorGameId = gameSession.getVendorGameId();
                Long vendorPlayerId = gameSession.getVendorPlayerId();
                List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(roundId, vendorGameId, vendorPlayerId);
                loggingService.logDataFlowByVendor("Inside notifyEndRoundAsync 3", settledBetVendorId, settledBetRoundId, unsettledBetList);

                unsettledBetList = this.filterFailedUnsettledBet(unsettledBetList);
                loggingService.logDataFlowByVendor("Inside notifyEndRoundAsync 5", settledBetVendorId, settledBetRoundId, unsettledBetList);

                // multiple bets within same round
                for (UnsettledBet betRecord : unsettledBetList) {
                    if (!settledBet.getId().equals(betRecord.getId())) { // exclude the current bet record
                        loggingService.logDataFlowByVendor("Inside notifyEndRoundAsync 6", settledBetVendorId, settledBetRoundId, betRecord);
                        final String newTraceId = UUID.randomUUID().toString();

                        //if unsettledBet data do have settledTime, then do not update by latest settledTime (PGSOFT CHANGES)
                        if (betRecord.getVendorSettleTime() == null || betRecord.getVendorSettleTime() == 0) {
                            betRecord.setVendorSettleTime(settledBet.getVendorSettleTime());
                        }

                        SettledBet newSettledBet = new SettledBet(betRecord, vendorService, newTraceId);
                        loggingService.logDataFlowByVendor("Inside notifyEndRoundAsync 7", settledBetVendorId, settledBetRoundId, newSettledBet);

                        //if no result time then will set it as settle time
                        if (newSettledBet.getResultTime() == null) {
                            newSettledBet.setResultTime(newSettledBet.getVendorSettleTime());
                        }

                        //AgentPlayerUsername, CurrencyCode and GameCode is used for walletBetResultAction.call when process end round result for operator
                        EndRoundSettledBet endRoundSettledBet = new EndRoundSettledBet(newSettledBet, gameSession.getAgentPlayerUsername(),
                                gameSession.getCurrencyCode(), gameSession.getGameCode());
                        endRoundSettledBet.setInternalTransactionId(newTraceId);

                        loggingService.logDataFlowByVendor("Before produceEndRoundSettleBet", settledBetVendorId, settledBetRoundId, endRoundSettledBet);
//                        kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
                        endRoundProcessor.process(newSettledBet, betRecord, endRoundSettledBet, gameSession);
                        loggingService.logDataFlowByVendor("After produceEndRoundSettleBet", settledBetVendorId, settledBetRoundId, endRoundSettledBet);
                    }
                }
            } catch (Exception exception) {
                log.error("[{}] notifyEndRoundAsync -> {}", traceId, exception.getMessage());
            }
        }, Instant.now().plusSeconds(5)); // use ThreadPoolTaskScheduler set delay schedule to process EndRound later (5 seconds delay)
    }

    public void executeRetryEndRound(SettledBet settledBet, BaseVendorService vendorService, GameSession gameSession, String traceId, int remainingAttempts) {
        try {
            remainingAttempts--;

            String redisKey = String.format(RedisKeyConstant.END_ROUND_REDIS_KEY, settledBet.getRoundId(), settledBet.getVendorGameId(), settledBet.getVendorPlayerId());
            List<UnsettledBet> unsettledBetList = unsettledBetService.getByRoundId(settledBet.getRoundId(), settledBet.getVendorGameId(), settledBet.getVendorPlayerId());
            loggingService.logDataFlowByVendor("Inside executeRetryEndRound 1", settledBet.getVendorId(), settledBet.getRoundId(), unsettledBetList);

            if (remainingAttempts <= 0) {
                redisTemplate.delete(redisKey);
                loggingService.logDataFlowByVendor("Inside executeRetryEndRound 2", settledBet.getVendorId(), settledBet.getRoundId(), unsettledBetList);
                processEndRound(settledBet, unsettledBetList, vendorService, gameSession, traceId);
                return;
            }

            int redisUnsettledBetCount = Objects.requireNonNullElse(redisTemplate.opsForSet().size(redisKey), 0L).intValue();
            loggingService.logDataFlowByVendor("Inside executeRetryEndRound redis value", settledBet.getVendorId(), settledBet.getRoundId(), redisTemplate.opsForSet().members(redisKey));
            boolean isMatched = redisUnsettledBetCount != 0 && redisUnsettledBetCount == unsettledBetList.size();

            // if redisUnsettledBetCount is null, mean vendor send endRound after 2 hours of redis key TTL (will proceed to process endRound)
            if (redisUnsettledBetCount == 0 || isMatched) {
                redisTemplate.delete(redisKey);
                loggingService.logDataFlowByVendor("Inside executeRetryEndRound 3", settledBet.getVendorId(), settledBet.getRoundId(), unsettledBetList);
                processEndRound(settledBet, unsettledBetList, vendorService, gameSession, traceId);
            } else {
                String endRoundRetryCounterRedisKey = String.format(RedisKeyConstant.END_ROUND_RETRY_COUNTER_REDIS_KEY, settledBet.getRoundId(), settledBet.getVendorGameId(), settledBet.getVendorPlayerId());
                redisTemplate.opsForValue().increment(endRoundRetryCounterRedisKey);
                redisTemplate.expire(endRoundRetryCounterRedisKey, 5L, TimeUnit.MINUTES);
                final int finalRetryCount = remainingAttempts;
                loggingService.logDataFlowByVendor("Inside executeRetryEndRound redis retry count", settledBet.getVendorId(), settledBet.getRoundId(), redisTemplate.opsForValue().get(endRoundRetryCounterRedisKey));
                loggingService.logDataFlowByVendor("Inside executeRetryEndRound 4", settledBet.getVendorId(), settledBet.getRoundId(), settledBet);
                taskScheduler.schedule(() -> executeRetryEndRound(settledBet, vendorService, gameSession, traceId, finalRetryCount), Instant.now().plusSeconds(this.retryIntervalInSecondsValue));
            }
        } catch (Exception exception) {
            log.error("[{}] executeRetryEndRound -> {}", traceId, exception.getMessage());
        }
    }

    private void processEndRound(SettledBet settledBet, List<UnsettledBet> unsettledBetList, BaseVendorService vendorService, GameSession gameSession, String traceId) {
        try {
            unsettledBetList = this.filterFailedUnsettledBet(unsettledBetList);
            loggingService.logDataFlowByVendor("Inside processEndRound 1", settledBet.getVendorId(), settledBet.getRoundId(), unsettledBetList);

            // multiple bets within same round
            for (UnsettledBet betRecord : unsettledBetList) {
                if (!settledBet.getId().equals(betRecord.getId())) { // exclude the current bet record
                    final String newTraceId = UUID.randomUUID().toString();

                    //if unsettledBet data do have settledTime, then do not update by latest settledTime (PGSOFT CHANGES)
                    if (betRecord.getVendorSettleTime() == null || betRecord.getVendorSettleTime() == 0) {
                        betRecord.setVendorSettleTime(settledBet.getVendorSettleTime());
                    }

                    if (WalletServiceConstant.updateSettleTimeVendorList.contains(betRecord.getVendorId())) {
                        //if it's game from habanero and MG
                        betRecord.setVendorSettleTime(settledBet.getVendorSettleTime());
                    }

                    SettledBet newSettledBet = new SettledBet(betRecord, vendorService, newTraceId);

                    //if no result time then will set it as settle time
                    if (newSettledBet.getResultTime() == null) {
                        newSettledBet.setResultTime(newSettledBet.getVendorSettleTime());
                    }

                    //AgentPlayerUsername, CurrencyCode and GameCode is used for walletBetResultAction.call when process end round result for operator
                    EndRoundSettledBet endRoundSettledBet = new EndRoundSettledBet(newSettledBet, gameSession.getAgentPlayerUsername(),
                            gameSession.getCurrencyCode(), gameSession.getGameCode());
                    endRoundSettledBet.setInternalTransactionId(newTraceId);

//                    kafkaService.produceEndRoundSettleBet(endRoundSettledBet);
                    endRoundProcessor.process(newSettledBet, betRecord, endRoundSettledBet, gameSession);
                }
            }
        } catch (Exception exception) {
            log.error("[{}] notifyEndRoundAsync -> {}", traceId, exception.getMessage());
        }
    }

    private List<UnsettledBet> filterFailedUnsettledBet(List<UnsettledBet> unsettledBetList) {
        if (unsettledBetList == null || unsettledBetList.isEmpty()) {
            return unsettledBetList;
        }

        return unsettledBetList.stream()
                .filter(unsettledBet -> unsettledBet.getOperatorStatus().equals(ResponseCodes.Status.SC_OK.code))
                .toList();
    }
}
