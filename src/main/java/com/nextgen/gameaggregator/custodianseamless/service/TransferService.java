package com.nextgen.gameaggregator.custodianseamless.service;

import com.nextgen.gameaggregator.custodianseamless.exception.DuplicateReferenceIdException;
import com.nextgen.gameaggregator.custodianseamless.exception.TransferHistoryNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.operator.deposit.TransferData;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TraceIdRequest;
import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.wallet.reader.AccessKeyReaderRepository;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TransferService {

    @Autowired
    private AgentPlayerRepository agentPlayerRepository;

    @Autowired
    private GameUrlService gameUrlService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AccessKeyReaderRepository accessKeyReaderRepository;

    @Autowired
    private TransferHistoryService transferHistoryService;

    @Autowired
    private KafkaService kafkaService;

    @Cacheable(value = "TraceIds", key = "{#traceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public TraceIdRequest checkTraceIdExists(String traceId, Integer agentId) throws DuplicateRequestException {
        String cacheKey = "TraceIds::" + traceId + "," + agentId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            throw new DuplicateRequestException("traceId :" + traceId + " existing within 2 hours ");
        } else {
            return new TraceIdRequest(traceId, agentId);
        }
    }

    public RawTransferHistory checkReferenceIdExists(String referenceId, Integer agentId) throws DuplicateReferenceIdException {
        RawTransferHistory rawTransferHistory = transferHistoryService.getTransactionHistoryById(referenceId, agentId);
        if (rawTransferHistory != null) {
            throw new DuplicateReferenceIdException("referenceId :" + referenceId + " existing within 7 days ");
        } else {
            return rawTransferHistory;
        }
    }

    public RawTransferHistory getTransferHistoryByReferenceId(String referenceId, Integer agentId, Currency currency, String username )
            throws  TransferHistoryNotFoundException {
        RawTransferHistory rawTransferHistory = transferHistoryService.getTransactionHistoryById(referenceId, agentId);
        Optional.ofNullable(rawTransferHistory).orElseThrow(TransferHistoryNotFoundException::new);

        if (!rawTransferHistory.getCurrencyId().equals(currency.getId())) {
            throw new TransferHistoryNotFoundException();
        }

        if (!rawTransferHistory.getAgentPlayerUsername().equals(username)) {
            throw new TransferHistoryNotFoundException();
        }

        return rawTransferHistory;

    }


    @CachePut(value = "AgentPlayers", key = "{#agent.id, #username}", cacheManager = "cacheManager")
    public AgentPlayer checkAgentPlayer(Agent agent, String username) throws DisabledAgentPlayerException {
        AgentPlayer agentPlayer = agentPlayerRepository.findByAgentIdAndUsername(agent.getId(), username);
        if (agentPlayer == null) {
            agentPlayer = gameUrlService.createAgentPlayer(agent.getId(), username);
            agentPlayerRepository.save(agentPlayer);
        } else {
            if (agentPlayer.getStatus().equals(Status.INACTIVE.code)) {
                throw new DisabledAgentPlayerException();
            }
        }
        return agentPlayer;
    }

    @Cacheable(value = "AccessKeys", cacheManager = "cacheManager", unless = "#result == null")
    public AccessKey getWalletServiceAccessKey() throws WalletServiceAccessKeyNotFoundException {
        AccessKey accessKey = accessKeyReaderRepository.findFirstByOrderById();
        Optional.ofNullable(accessKey).orElseThrow(() ->
                new WalletServiceAccessKeyNotFoundException(ResponseCodes.Status.SC_INTERNAL_ERROR.code));

        return accessKey;
    }

    public TransferData saveTransactionHistory(RawTransferHistory rawTransferHistory, Currency currency) {

        if (rawTransferHistory.getTransactionId() == null) {
            rawTransferHistory.setTransactionId(UUID.randomUUID().toString());
        }

        transferHistoryService.updateRawTransferHistory(rawTransferHistory);
        transferHistoryService.saveRawTransferHistory(rawTransferHistory);
        //send to process transfer history kafka topic
        kafkaService.produceTransferHistory(rawTransferHistory);

        return new TransferData(rawTransferHistory, currency.getCode());

    }


}
