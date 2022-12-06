package com.nextgen.gameaggregator.vendor.grpc.v1.subcriber;

import com.nextgen.gameaggregator.grpc.v1.operator.walletbalance.WalletBalanceGrpcDto;
import com.nextgen.gameaggregator.grpc.v1.operator.walletbalance.WalletBalanceGrpcVo;
import com.nextgen.gameaggregator.grpc.v1.operator.walletbalance.WalletBalanceServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OperatorWalletBalanceGrpc {

    @GrpcClient("game-aggregator-operator-api-service")
    private WalletBalanceServiceGrpc.WalletBalanceServiceBlockingStub blockingStub;

    public WalletBalanceGrpcVo walletBalance(
            final Long agentId,
            final Long agentPlayerId,
            final Long vendorId,
            final String currency,
            final String traceId,
            final Long agentCredentialId
    ) {

        WalletBalanceGrpcDto dto = WalletBalanceGrpcDto.newBuilder()
                .setAgentId(agentId)
                .setAgentPlayerId(agentPlayerId)
                .setVendorId(vendorId)
                .setCurrency(currency)
                .setTraceId(traceId)
                .setAgentCredentialId(agentCredentialId)
                .build();

        return blockingStub.walletBalance(dto);

    }
}
