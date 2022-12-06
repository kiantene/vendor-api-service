package com.nextgen.gameaggregator.vendor.grpc.v1.subcriber;

import com.nextgen.gameaggregator.grpc.v1.operator.betrequest.BetRequestGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.operator.betrequest.BetRequestGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.operator.betrequest.BetRequestServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OperatorBetRequestGrpc {

    @GrpcClient("game-aggregator-operator-api-service")
    private BetRequestServiceGrpc.BetRequestServiceBlockingStub blockingStub;

    public BetRequestGrpcVo betRequest(
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
            final Long betTime
    ){
        BetRequestGrpcDto dto = BetRequestGrpcDto.newBuilder()
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
                .setBetTime(betTime)
                .build();

        return blockingStub.betRequest(dto);
    }
}
