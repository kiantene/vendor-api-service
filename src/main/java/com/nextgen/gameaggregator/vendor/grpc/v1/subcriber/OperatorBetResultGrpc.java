package com.nextgen.gameaggregator.vendor.grpc.v1.subcriber;

import com.nextgen.gameaggregator.grpc.v1.operator.betresult.BetResultGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.operator.betresult.BetResultGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.operator.betresult.BetResultServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OperatorBetResultGrpc {
    @GrpcClient("game-aggregator-operator-api-service")
    private BetResultServiceGrpc.BetResultServiceBlockingStub blockingStub;

    public BetResultGrpcVo betResult(
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
        BetResultGrpcDto dto = BetResultGrpcDto.newBuilder()
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
