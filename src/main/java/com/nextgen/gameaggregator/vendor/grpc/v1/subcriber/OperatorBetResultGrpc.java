package com.nextgen.gameaggregator.vendor.grpc.v1.subcriber;

import com.nextgen.gameaggregator.grpc.v1.operator.betresult.BetResultDto;
import com.nextgen.gameaggregator.grpc.v1.operator.betresult.BetResultServiceGrpc;
import com.nextgen.gameaggregator.grpc.v1.operator.betresult.BetResultVo;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OperatorBetResultGrpc {
    @GrpcClient("game-aggregator-operator-api-service")
    private BetResultServiceGrpc.BetResultServiceBlockingStub blockingStub;

    public BetResultVo betResult(
            final Long agentId,
            final Long agentPlayerId,
            final Long gameId,
            final String currency,
            final String traceId,
            final Long agentCredentialId,
            final String betId,
            final String externalBetId,
            final String externalRoundId,
            final Double betAmount,
            final Double winLoss,
            final Integer resultType,
            final Long betTime,
            final Long settledTime
    ){
        BetResultDto dto = BetResultDto.newBuilder()
                .setAgentId(agentId)
                .setAgentPlayerId(agentPlayerId)
                .setGameId(gameId)
                .setCurrency(currency)
                .setTraceId(traceId)
                .setAgentCredentialId(agentCredentialId)
                .setBetId(betId)
                .setExternalBetId(externalBetId)
                .setExternalRoundId(externalRoundId)
                .setBetAmount(betAmount)
                .setWinLoss(winLoss)
                .setResultType(resultType)
                .setBetTime(betTime)
                .setSettledTime(settledTime)
                .build();

        return blockingStub.betResult(dto);
    }
}
