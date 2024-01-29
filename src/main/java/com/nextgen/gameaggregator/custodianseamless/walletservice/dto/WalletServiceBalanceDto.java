package com.nextgen.gameaggregator.custodianseamless.walletservice.dto;

import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.entity.ga.Currency;
import lombok.Data;

@Data
public class WalletServiceBalanceDto {

    private String traceId;
    private String username;
    private Long playerId;
    private Integer entityId;
    private Integer tokenId;

    public WalletServiceBalanceDto(String traceId, AgentPlayer agentPlayer, Currency currency){
        this.traceId = traceId;
        this.username = agentPlayer.getUsername();
        this.playerId = agentPlayer.getId();
        this.entityId = agentPlayer.getAgentId();
        this.tokenId = currency.getId();
    }
}
