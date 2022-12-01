package com.nextgen.gameaggregator.vendor.grpc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VendorGameLoginServiceRequestDto {
    private long agentPlayerId;
    private long vendorCredentialId;
    private long vendorId;
    private long gameId;
    private String language;
    private String platform;
    private String currency;
    private long agentId;
    private String playerUsername;
    private long houseId;
    private long masterAgentId;
    private String traceId;
    private long walletType;

}
