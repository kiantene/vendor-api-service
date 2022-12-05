package com.nextgen.gameaggregator.vendor.grpc.v1.subcriber;

import com.nextgen.gameaggregator.grpc.v1.operator.betrefund.BetRefundGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.operator.betrefund.BetRefundGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.operator.betrefund.BetRefundServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OperatorBetRefundGrpc {

    @GrpcClient("game-aggregator-operator-api-service")
    private BetRefundServiceGrpc.BetRefundServiceBlockingStub blockingStub;

    public BetRefundGrpcVo betRefund(
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
            final Double refundAmount,
            final Integer resultType,
            final Long betTime,
            final Long settledTime
    ){
        BetRefundGrpcDto dto = BetRefundGrpcDto.newBuilder()
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
                .setRefundAmount(refundAmount)
                .setResultType(resultType)
                .setBetTime(betTime)
                .setSettledTime(settledTime)
                .build();

        return blockingStub.betRefund(dto);
    }



}
