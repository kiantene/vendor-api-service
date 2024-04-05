package com.nextgen.gameaggregator.custodianseamless.walletservice.dto;

import com.nextgen.gameaggregator.entity.ga.RawTransferHistory;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletServiceTransferDto {

    private String traceId;
    private String referenceId;
    private String username;
    private Long playerId;
    private Integer entityId;
    private Integer walletType;
    private Integer transactionType;
    private Integer tokenId;
    private BigDecimal amount;
    private Long timestamp;


    public WalletServiceTransferDto(String traceId, RawTransferHistory rawTransferHistory, Integer transactionType){

        this.traceId = traceId;
        this.referenceId = rawTransferHistory.getReferenceId();
        this.username = rawTransferHistory.getAgentPlayerUsername();
        this.playerId = rawTransferHistory.getAgentPlayerId();
        this.entityId = rawTransferHistory.getAgentId();
        this.walletType = 1; //main wallet
        this.transactionType = transactionType; //1 = Deposit, 2 = withdraw
        this.tokenId = rawTransferHistory.getCurrencyId();
        this.amount = rawTransferHistory.getTransferAmount();
        this.timestamp = rawTransferHistory.getCreateTime();


    }

}
