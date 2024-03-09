package com.nextgen.gameaggregator.custodianseamless.service;

import com.nextgen.gameaggregator.custodianseamless.constant.TransactionStatus;
import com.nextgen.gameaggregator.custodianseamless.constant.TransactionType;
import com.nextgen.gameaggregator.custodianseamless.exception.DuplicateReferenceIdException;
import com.nextgen.gameaggregator.custodianseamless.exception.TransferHistoryNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.operator.deposit.TransferData;
import com.nextgen.gameaggregator.custodianseamless.operator.dto.TraceIdRequest;
import com.nextgen.gameaggregator.custodianseamless.walletservice.vo.BalanceBeforeAfterVo;
import com.nextgen.gameaggregator.entity.ga.Agent;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.entity.wallet.TransferHistory;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.game.url.GameUrlService;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import com.nextgen.gameaggregator.repository.wallet.reader.AccessKeyReaderRepository;
import com.nextgen.gameaggregator.repository.wallet.reader.TransferHistoryReaderRepository;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    @Autowired
    private TransferHistoryReaderRepository transferHistoryReaderRepository;

    @Cacheable(value = "TraceIds", key = "{#traceId, #agentId}", cacheManager = "cacheManager", unless = "#result == null")
    public TraceIdRequest checkTraceIdExists(String traceId, Integer agentId) throws DuplicateRequestException {
        String cacheKey = "TraceIds::" + traceId + "," + agentId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            throw new DuplicateRequestException("traceId :" + traceId + " existing within 2 hours ");
        } else {
            return new TraceIdRequest(traceId, agentId);
        }
    }

    public void  checkReferenceIdExists(String referenceId, Integer agentId) throws DuplicateReferenceIdException {
        Optional<RawTransferHistory> rawTransferHistory = transferHistoryService.getTransactionHistoryById(referenceId, agentId);
        if (rawTransferHistory.isPresent()) {
            throw new DuplicateReferenceIdException("referenceId :" + referenceId + " existing within 1 days ");
        }
    }


    public TransferHistory getTransferHistoryByReferenceId(String referenceId, Integer agentId, Currency currency, String username)
            throws TransferHistoryNotFoundException {
        Optional<RawTransferHistory> rawTransferHistory =
                transferHistoryService.getTransactionHistoryById(referenceId, agentId);

        TransferHistory transferHistory = null;
        if (rawTransferHistory.isPresent()) {
            transferHistory =  new TransferHistory(rawTransferHistory.get());
        }else{
            transferHistory = transferHistoryReaderRepository.findTransferHistoriesByAgentIdAndReferenceId(agentId, referenceId);
        }

        Optional.ofNullable(transferHistory).orElseThrow(TransferHistoryNotFoundException::new);

        if (!transferHistory.getCurrencyId().equals(currency.getId())) {
            throw new TransferHistoryNotFoundException();
        }

        if (!transferHistory.getAgentPlayerUsername().equals(username)) {
            throw new TransferHistoryNotFoundException();
        }

        return transferHistory;

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

        transferHistoryService.updateRawTransferHistory(rawTransferHistory);
        transferHistoryService.saveRawTransferHistory(rawTransferHistory);
        //send to process transfer history kafka topic
        kafkaService.produceTransferHistory(new TransferHistory(rawTransferHistory));

        return new TransferData(rawTransferHistory, currency.getCode());
    }


    public RawTransferHistory mapWalletServiceResponse(RawTransferHistory rawTransferHistory,
                                                       BalanceBeforeAfterVo balanceBeforeAfterVo,
                                                       Integer transactionType) throws InvalidResponseException {


            //validate wallet service should only response SC_OK and SC_INSUFFICIENT_FUNDS status
        if ((!balanceBeforeAfterVo.getStatus().equals(ResponseCodes.Status.SC_OK)) &&
                (!balanceBeforeAfterVo.getStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS))) {
            throw new InvalidResponseException("Invalid Wallet Service Response Code :" + balanceBeforeAfterVo.getStatus());


            //validate SC_OK response body
        } else if ((balanceBeforeAfterVo.getStatus().equals(ResponseCodes.Status.SC_OK)) &&
                ((balanceBeforeAfterVo.getData().getTransactionId().isEmpty()) ||
                        (balanceBeforeAfterVo.getData().getCompletedAt() == null) ||
                        (balanceBeforeAfterVo.getData().getBalanceAfter() == null) ||
                        (balanceBeforeAfterVo.getData().getBalanceBefore() == null))) {

            throw new InvalidResponseException("Invalid Wallet Service Response Value for " +
                    TransactionType.getTransactionTypeByStatus(transactionType));

            //only check when is withdrawal and response status is insufficient fund
        } else if ((transactionType.equals(TransactionType.WITHDRAWAL.status)) &&
                (balanceBeforeAfterVo.getStatus().equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS)) &&
                ((!balanceBeforeAfterVo.getData().getTransactionId().isEmpty()) ||
                        (balanceBeforeAfterVo.getData().getCompletedAt() == null) ||
                        (balanceBeforeAfterVo.getData().getBalanceAfter() == null) ||
                        (balanceBeforeAfterVo.getData().getBalanceBefore() == null)
                )) {
            throw new InvalidResponseException("Invalid Wallet Service Response Value for withdrawal insufficient funds ");

            //validate response username and currency is match with request
        } else if ((!balanceBeforeAfterVo.getData().getUsername().equals(rawTransferHistory.getAgentPlayerUsername())) ||
                (!balanceBeforeAfterVo.getData().getTokenId().equals(rawTransferHistory.getCurrencyId()))
        ) {
            throw new InvalidResponseException("Invalid Wallet Service Response Value compare with request param ");
        }


        if (balanceBeforeAfterVo.getStatus().equals(ResponseCodes.Status.SC_OK)) {
            rawTransferHistory.setWalletTransactionId(balanceBeforeAfterVo.getData().getTransactionId().get(0));
        }

        rawTransferHistory.setResultTime(balanceBeforeAfterVo.getData().getCompletedAt());
        rawTransferHistory.setBalanceAfter(balanceBeforeAfterVo.getData().getBalanceAfter());
        rawTransferHistory.setBalanceBefore(balanceBeforeAfterVo.getData().getBalanceBefore());
        rawTransferHistory.setTransactionStatus(
                (balanceBeforeAfterVo.getStatus().equals(ResponseCodes.Status.SC_OK)) ?
                        TransactionStatus.SUCCESS.status : TransactionStatus.FAIL.status);

        return rawTransferHistory;

    }


}
